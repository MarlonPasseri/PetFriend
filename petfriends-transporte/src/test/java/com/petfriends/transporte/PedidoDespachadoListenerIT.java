package com.petfriends.transporte;

import com.petfriends.transporte.domain.Entrega;
import com.petfriends.transporte.domain.EntregaRepository;
import com.petfriends.shared.events.EventRouting;
import com.petfriends.shared.events.PedidoDespachadoEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Teste de integração com RabbitMQ real (Testcontainers): publica um
 * PedidoDespachadoEvent na exchange e verifica que o listener consumiu e
 * criou a Entrega correspondente em trânsito.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)   // pula se o Docker não estiver acessível
class PedidoDespachadoListenerIT {

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer("rabbitmq:3.13-management");

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    EntregaRepository repository;

    @Test
    void criaEntregaAoReceberPedidoDespachado() {
        UUID pedidoId = UUID.randomUUID();
        var endereco = new PedidoDespachadoEvent.EnderecoEntregaDto(
                "Rua das Flores", "100", "Apto 12", "Centro", "Sao Paulo", "SP", "01000-000");
        var evento = new PedidoDespachadoEvent(
                UUID.randomUUID(), pedidoId, UUID.randomUUID(), Instant.now(), endereco);

        rabbitTemplate.convertAndSend(EventRouting.EXCHANGE, EventRouting.RK_PEDIDO_DESPACHADO, evento);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var entrega = repository.buscarPorPedido(pedidoId).orElseThrow();
            assertThat(entrega.getStatus()).isEqualTo(Entrega.StatusEntrega.EM_TRANSITO);
            assertThat(entrega.getEndereco().cidade()).isEqualTo("Sao Paulo");
        });
    }
}
