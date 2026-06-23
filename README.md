# PetFriends — Microsserviços event-driven com DDD

> Decomposição do monólito **PetFriends** em microsserviços que se comunicam por
> **eventos de domínio** (assíncrono, via RabbitMQ), aplicando princípios de
> **Domain-Driven Design** e técnicas de decomposição.

Projeto multi-módulo Maven (Spring Boot 3.2 / Java 17). O módulo web (`PetFriends_Web`,
ReactJS) acessa Clientes, Produtos e Pedidos de forma **síncrona** via REST; já o
`PetFriends_Pedidos` notifica `PetFriends_Almoxarifado` e `PetFriends_Transporte` de
forma **assíncrona**, por eventos — exatamente a seta vermelha dos diagramas conceituais.

---

## Sumário

1. [Contexto](#contexto)
2. [Arquitetura](#arquitetura)
3. [Modelagem DDD](#modelagem-ddd)
4. [Domain Events — respostas dissertativas](#domain-events--respostas-dissertativas)
5. [Mapa: questão da prova → artefato](#mapa-questão-da-prova--artefato)
6. [Como executar](#como-executar)
7. [Console interativo](#console-interativo)
8. [Testes e qualidade](#testes-e-qualidade)
9. [Stack técnica](#stack-técnica)
10. [Estrutura de pastas](#estrutura-de-pastas)

---

## Contexto

O sistema modela o ciclo de vida do agregado **Pedido**, cujo diagrama de estados é:

```
E-Commerce → Novo ──[pagamento confirmado]──▶ Fechado ──Enviar Pedido──▶ Em Preparação
                                                 │                            │ (Almoxarifado)
                                          Cancelar Pedido            Despachar Pedido
                                                 ▼                            ▼
                                            Cancelado              Em Trânsito ──[recebido]──▶ Entregue
                                                                    │   │  (Transporte)
                                                   [decorridos 30 dias]  [rejeitado]
                                                          ▼                  ▼
                                                     Extraviado          Devolvido
```

Dois contextos delimitados (*bounded contexts*) reagem às transições do Pedido:

- **PetFriends_Almoxarifado** — controla estoque; reserva itens quando o pedido entra em preparação.
- **PetFriends_Transporte** — gerencia a entrega; cria a remessa quando o pedido é despachado.

---

## Arquitetura

### Módulos

| Módulo | Papel | Porta |
|---|---|---|
| [`petfriends-shared-events`](petfriends-shared-events) | Contratos dos eventos de domínio (biblioteca compartilhada) | — |
| [`petfriends-pedidos`](petfriends-pedidos) | Publica os eventos do agregado Pedido (lado produtor) | 8080 |
| [`petfriends-almoxarifado`](petfriends-almoxarifado) | Consome `pedido.confirmado` e reserva estoque (`ItemEstoque`) | 8081 |
| [`petfriends-transporte`](petfriends-transporte) | Consome `pedido.despachado` e cria a `Entrega` | 8082 |

### Fluxo event-driven

```
                         ┌───────────────────────────── RabbitMQ ─────────────────────────────┐
                         │                      TopicExchange: pedidos.exchange                │
                         │                                                                     │
  PetFriends_Pedidos ───▶│  rk=pedido.confirmado ──▶ [almoxarifado.pedido-confirmado.queue] ──┼─▶ Almoxarifado → reserva estoque
   (REST 8080)           │  rk=pedido.despachado ──▶ [transporte.pedido-despachado.queue]  ───┼─▶ Transporte  → cria Entrega
                         │                              (cada fila com sua DLQ)                │
                         └─────────────────────────────────────────────────────────────────────┘
```

- **Topologia AMQP:** uma `TopicExchange` `pedidos.exchange`; cada consumidor tem **fila própria + Dead Letter Queue (DLQ)**, ligada por sua *routing key*. Mensagens que falham após 3 retentativas são isoladas na DLQ em vez de perdidas ou reprocessadas para sempre.
- **Serialização:** eventos trafegam em JSON (`Jackson2JsonMessageConverter` com `JavaTimeModule`, datas em ISO-8601).

---

## Modelagem DDD

### Agregados (Aggregate Roots)

| Contexto | Agregado | Invariante protegido | Arquivo |
|---|---|---|---|
| Almoxarifado | `ItemEstoque` | Nunca reservar/baixar mais do que está disponível | [ItemEstoque.java](petfriends-almoxarifado/src/main/java/com/petfriends/almoxarifado/domain/ItemEstoque.java) |
| Transporte | `Entrega` | Transições de estado válidas (Criada→Em Trânsito→Entregue/Extraviada/Devolvida) | [Entrega.java](petfriends-transporte/src/main/java/com/petfriends/transporte/domain/Entrega.java) |

### Value Objects

| VO | Papel | Arquivo |
|---|---|---|
| `Quantidade` | Quantidade não-negativa, imutável | [Quantidade.java](petfriends-almoxarifado/src/main/java/com/petfriends/almoxarifado/domain/Quantidade.java) |
| `SKU` | Identificador de produto com formato validado | [SKU.java](petfriends-almoxarifado/src/main/java/com/petfriends/almoxarifado/domain/SKU.java) |
| `EnderecoEntrega` | Endereço coeso, validado (UF, CEP) | [EnderecoEntrega.java](petfriends-transporte/src/main/java/com/petfriends/transporte/domain/EnderecoEntrega.java) |

### Repositories (portas do domínio)

Cada repository expõe operações apenas sobre o *aggregate root*; a implementação JPA fica na infraestrutura (padrão *Ports & Adapters*).

- [`ItemEstoqueRepository`](petfriends-almoxarifado/src/main/java/com/petfriends/almoxarifado/domain/ItemEstoqueRepository.java) → impl. [ItemEstoqueRepositoryJpa](petfriends-almoxarifado/src/main/java/com/petfriends/almoxarifado/infra/persistence/ItemEstoqueRepositoryJpa.java)
- [`EntregaRepository`](petfriends-transporte/src/main/java/com/petfriends/transporte/domain/EntregaRepository.java) → impl. [EntregaRepositoryJpa](petfriends-transporte/src/main/java/com/petfriends/transporte/infra/persistence/EntregaRepositoryJpa.java)

---

## Domain Events — respostas dissertativas

### 2.1 — Qual funcionalidade síncrona do cliente é diretamente afetada pelos eventos de domínio?

A **consulta de disponibilidade/estoque do produto** (o `PetFriends_Web` lendo de
`PetFriends_Produtos`/estoque de forma síncrona via REST).

A reserva e a baixa de estoque acontecem de forma **assíncrona**: ao confirmar o pedido,
`PetFriends_Pedidos` emite um evento que o `PetFriends_Almoxarifado` consome para reservar
o estoque. Logo, a quantidade disponível que o cliente vê é **eventualmente consistente** —
entre a confirmação e o processamento do evento, a leitura síncrona pode mostrar um valor
desatualizado (ex.: produto ainda “disponível” que já foi todo reservado).

### 2.2 — Diferença entre enviar só o ID do agregado e enviar o payload completo

| | Só o ID (*thin event*) | Payload completo (*event-carried state transfer*) |
|---|---|---|
| Acoplamento | Maior — consumidor faz **callback síncrono** ao produtor para buscar os dados | Menor — consumidor é **autônomo**, não chama de volta |
| Disponibilidade | Depende do produtor estar no ar no consumo | Funciona mesmo se o produtor estiver indisponível |
| Frescor dos dados | Sempre frescos (lidos na hora) | Risco de dados **defasados**; exige versionamento |
| Tamanho / duplicação | Mensagem pequena, sem duplicação | Mensagem maior, **duplica** dados |

Em resumo: **só ID** favorece consistência ao custo de acoplamento e mais chamadas;
**payload completo** favorece autonomia e desacoplamento ao custo de duplicação e dados
potencialmente defasados.

### 2.3 — Evento `PetFriends_Pedidos → PetFriends_Almoxarifado`

O Almoxarifado precisa **reservar estoque por item**. Para evitar um callback síncrono ao
Pedidos (que reintroduziria acoplamento), projeto um evento com **payload completo**
contendo os itens (SKU + quantidade) — veja
[PedidoConfirmadoEvent.java](petfriends-shared-events/src/main/java/com/petfriends/shared/events/PedidoConfirmadoEvent.java):

```json
{
  "eventId": "…", "pedidoId": "…", "ocorridoEm": "2026-06-23T10:00:00Z",
  "itens": [ { "sku": "RACAO-001", "quantidade": 2 } ]
}
```

### 2.4 — Evento `PetFriends_Pedidos → PetFriends_Transporte`

O Transporte precisa **criar a entrega** com o endereço de destino. Novamente payload
completo, para montar a `Entrega` sem callback — veja
[PedidoDespachadoEvent.java](petfriends-shared-events/src/main/java/com/petfriends/shared/events/PedidoDespachadoEvent.java):

```json
{
  "eventId": "…", "pedidoId": "…", "clienteId": "…", "ocorridoEm": "2026-06-23T11:00:00Z",
  "endereco": { "logradouro": "…", "cidade": "São Paulo", "uf": "SP", "cep": "01000-000" }
}
```

> **Design dos eventos:** todos carregam `eventId` (idempotência em entregas *at-least-once*)
> e `ocorridoEm` (ordem temporal).

---

## Mapa: questão da prova → artefato

| Questão | Entregável | Arquivo |
|---|---|---|
| **DDD** — Entity + Repository do Almoxarifado | Agregado `ItemEstoque` + repositório | [ItemEstoque.java](petfriends-almoxarifado/src/main/java/com/petfriends/almoxarifado/domain/ItemEstoque.java), [ItemEstoqueRepository.java](petfriends-almoxarifado/src/main/java/com/petfriends/almoxarifado/domain/ItemEstoqueRepository.java) |
| **DDD** — Value Object do Almoxarifado | `Quantidade` / `SKU` | [Quantidade.java](petfriends-almoxarifado/src/main/java/com/petfriends/almoxarifado/domain/Quantidade.java), [SKU.java](petfriends-almoxarifado/src/main/java/com/petfriends/almoxarifado/domain/SKU.java) |
| **DDD** — Entity + Repository do Transporte | Agregado `Entrega` + repositório | [Entrega.java](petfriends-transporte/src/main/java/com/petfriends/transporte/domain/Entrega.java), [EntregaRepository.java](petfriends-transporte/src/main/java/com/petfriends/transporte/domain/EntregaRepository.java) |
| **DDD** — Value Object do Transporte | `EnderecoEntrega` | [EnderecoEntrega.java](petfriends-transporte/src/main/java/com/petfriends/transporte/domain/EnderecoEntrega.java) |
| **Eventos 2.1–2.2** | Respostas dissertativas | [seção acima](#domain-events--respostas-dissertativas) |
| **Eventos 2.3** | Evento p/ Almoxarifado | [PedidoConfirmadoEvent.java](petfriends-shared-events/src/main/java/com/petfriends/shared/events/PedidoConfirmadoEvent.java) |
| **Eventos 2.4** | Evento p/ Transporte | [PedidoDespachadoEvent.java](petfriends-shared-events/src/main/java/com/petfriends/shared/events/PedidoDespachadoEvent.java) |
| **Async 3.1** — Config de mensageria (Almoxarifado) | Exchange + fila + binding + DLQ | [AlmoxarifadoMessagingConfig.java](petfriends-almoxarifado/src/main/java/com/petfriends/almoxarifado/config/AlmoxarifadoMessagingConfig.java) |
| **Async 3.2** — Serviço que recebe os eventos (Almoxarifado) | `@RabbitListener` que reserva estoque | [PedidoConfirmadoListener.java](petfriends-almoxarifado/src/main/java/com/petfriends/almoxarifado/application/PedidoConfirmadoListener.java) |
| **Async 3.3** — Config de mensageria (Transporte) | Exchange + fila + binding + DLQ | [TransporteMessagingConfig.java](petfriends-transporte/src/main/java/com/petfriends/transporte/config/TransporteMessagingConfig.java) |
| **Async 3.4** — Serviço que recebe os eventos (Transporte) | `@RabbitListener` que cria a entrega | [PedidoDespachadoListener.java](petfriends-transporte/src/main/java/com/petfriends/transporte/application/PedidoDespachadoListener.java) |

---

## Como executar

### Opção A — Docker (um comando)

```bash
docker compose up --build
```

Sobe RabbitMQ + os 3 serviços; espera o broker ficar saudável (healthcheck) antes de
iniciar os consumidores; persiste o H2 em volumes nomeados. Painel do RabbitMQ em
http://localhost:15672 (guest/guest).

### Opção B — Maven (desenvolvimento)

```bash
docker compose up -d rabbitmq          # só o broker
mvn clean install                      # compila e instala os módulos

# em três terminais:
mvn -pl petfriends-almoxarifado spring-boot:run
mvn -pl petfriends-transporte  spring-boot:run
mvn -pl petfriends-pedidos     spring-boot:run
```

### Testando o fluxo via curl

```bash
# Confirmar pedido → Almoxarifado reserva estoque (SKU RACAO-001 já vem semeado)
curl -X POST http://localhost:8080/pedidos/confirmar \
  -H "Content-Type: application/json" \
  -d '{"eventId":"11111111-1111-1111-1111-111111111111",
       "pedidoId":"22222222-2222-2222-2222-222222222222",
       "ocorridoEm":"2026-06-23T10:00:00Z",
       "itens":[{"sku":"RACAO-001","quantidade":2}]}'

# Despachar pedido → Transporte cria a entrega
curl -X POST http://localhost:8080/pedidos/despachar \
  -H "Content-Type: application/json" \
  -d '{"eventId":"33333333-3333-3333-3333-333333333333",
       "pedidoId":"22222222-2222-2222-2222-222222222222",
       "clienteId":"44444444-4444-4444-4444-444444444444",
       "ocorridoEm":"2026-06-23T11:00:00Z",
       "endereco":{"logradouro":"Rua das Flores","numero":"100","complemento":"Apto 12",
                   "bairro":"Centro","cidade":"Sao Paulo","uf":"SP","cep":"01000-000"}}'

# Consultar resultado
curl http://localhost:8081/estoque        # estoque do Almoxarifado
curl http://localhost:8082/entregas       # entregas do Transporte
```

---

## Console interativo

Para demonstração, há um menu de terminal que dispara os eventos e mostra o resultado:

```bash
bash petfriends.sh
```

```
1) Confirmar pedido   → reserva estoque e mostra o saldo atualizado
2) Despachar pedido   → cria a entrega (reaproveita o último pedido)
3) Ver estoque        4) Ver entregas        5) Ver filas (RabbitMQ)
6) Testar DLQ (SKU inexistente)              s) Status dos serviços
```

---

## Testes e qualidade

```bash
mvn test      # testes unitários (agregados e Value Objects)
mvn verify    # unitários + integração (RabbitMQ real via Testcontainers)
```

- **Unitários (18):** invariantes do `ItemEstoque` (reserva nunca excede o disponível),
  máquina de estados da `Entrega`, validações de `Quantidade`, `SKU` e `EnderecoEntrega`.
- **Integração (Testcontainers):** publicam um evento em um RabbitMQ real e verificam a
  reserva de estoque / criação da entrega ponta a ponta
  ([PedidoConfirmadoListenerIT](petfriends-almoxarifado/src/test/java/com/petfriends/almoxarifado/PedidoConfirmadoListenerIT.java),
  [PedidoDespachadoListenerIT](petfriends-transporte/src/test/java/com/petfriends/transporte/PedidoDespachadoListenerIT.java)).
  Pulam automaticamente onde o Docker não estiver acessível (`disabledWithoutDocker`).
- **Schema versionado com Flyway:** o schema é criado por migrations
  ([Almoxarifado](petfriends-almoxarifado/src/main/resources/db/migration/V1__create_item_estoque.sql),
  [Transporte](petfriends-transporte/src/main/resources/db/migration/V1__create_entrega.sql))
  e o Hibernate roda em modo `validate` (não altera o banco).

---

## Stack técnica

- **Java 17**, **Spring Boot 3.2** (Web, AMQP, Data JPA)
- **RabbitMQ** (mensageria assíncrona, topic exchange + DLQ)
- **H2** (banco por serviço — *database per service*), **Flyway** (migrations)
- **Maven** multi-módulo; **Docker / Docker Compose**
- **JUnit 5 + AssertJ + Testcontainers** (testes)

---

## Estrutura de pastas

```
PETFRIEND/
├── pom.xml                       # POM pai (módulos + Failsafe)
├── docker-compose.yml            # broker + 3 serviços
├── Dockerfile                    # multi-stage parametrizado por MODULE
├── petfriends.sh                 # console interativo de demonstração
├── petfriends-shared-events/     # contratos dos eventos de domínio
├── petfriends-pedidos/           # produtor (REST + publisher)
├── petfriends-almoxarifado/      # domain · infra · config · application
└── petfriends-transporte/        # domain · infra · config · application
```

> Cada serviço de negócio segue a separação **domain** (agregados, VOs, repositórios) ·
> **infra** (persistência JPA) · **config** (mensageria) · **application** (listeners) ·
> **api** (REST de consulta).
