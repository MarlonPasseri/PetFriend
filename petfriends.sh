#!/usr/bin/env bash
# Console interativo do PetFriends — dispara eventos e inspeciona o resultado.
# Requer: curl e python (ambos já usados no projeto). Rode no Git Bash.

PEDIDOS="http://localhost:8080"
ALMOX="http://localhost:8081"
TRANSP="http://localhost:8082"
RABBIT="http://localhost:15672"
RUSER="guest"; RPASS="guest"

# cores
B="\033[1m"; G="\033[32m"; Y="\033[33m"; R="\033[31m"; C="\033[36m"; D="\033[2m"; N="\033[0m"

uuid() { python -c "import uuid;print(uuid.uuid4())"; }
LASTFILE=".petfriends_last_pedido"
save_last() { echo "$1" > "$LASTFILE"; }
get_last()  { cat "$LASTFILE" 2>/dev/null; }
pause() { echo; read -rp "$(echo -e "${D}<Enter> para voltar...${N}")" _; }

pretty() { python -c "import sys,json;print(json.dumps(json.load(sys.stdin),indent=2,ensure_ascii=False))" 2>/dev/null; }

# Faz POST lendo o corpo JSON do stdin (sempre ASCII puro vindo do python json.dumps,
# por isso acentos não quebram o encoding). Retorna o HTTP code.
post_event() {
  curl -s -o /dev/null -w "%{http_code}" -X POST "$PEDIDOS$1" \
    -H "Content-Type: application/json; charset=utf-8" --data-binary @-
}

check() {
  echo -e "${B}Status dos serviços${N}"
  for pair in "Pedidos:$PEDIDOS/pedidos" "Almoxarifado:$ALMOX/estoque" "Transporte:$TRANSP/entregas"; do
    name="${pair%%:*}"; url="${pair#*:}"
    if curl -s -o /dev/null -m 2 "$url"; then echo -e "  ${G}●${N} $name"; else echo -e "  ${R}○${N} $name (offline)"; fi
  done
  if curl -s -o /dev/null -m 2 -u "$RUSER:$RPASS" "$RABBIT/api/overview"; then
    echo -e "  ${G}●${N} RabbitMQ"; else echo -e "  ${R}○${N} RabbitMQ (offline)"; fi
}

confirmar() {
  echo -e "${B}Confirmar pedido${N} ${D}(reserva estoque no Almoxarifado)${N}"
  read -rp "  SKU [RACAO-001]: " sku;        sku=${sku:-RACAO-001}
  read -rp "  Quantidade [2]: " qtd;          qtd=${qtd:-2}
  pid=$(uuid); save_last "$pid"   # PedidoId gerado automaticamente
  code=$(PF_EID="$(uuid)" PF_PID="$pid" PF_SKU="$sku" PF_QTD="$qtd" python -c "
import os,json
print(json.dumps({'eventId':os.environ['PF_EID'],'pedidoId':os.environ['PF_PID'],
  'ocorridoEm':'2026-06-23T10:00:00Z',
  'itens':[{'sku':os.environ['PF_SKU'],'quantidade':int(os.environ['PF_QTD'])}]}))
" | post_event /pedidos/confirmar)
  echo -e "  → publicado (HTTP ${C}$code${N}) pedidoId=${C}$pid${N}"
  echo -e "  ${D}aguardando o Almoxarifado consumir...${N}"; sleep 2
  echo -e "  estoque do SKU ${C}$sku${N} agora:"
  curl -s "$ALMOX/estoque/$sku" | pretty | sed 's/^/    /'
}

despachar() {
  echo -e "${B}Despachar pedido${N} ${D}(cria entrega no Transporte)${N}"
  last=$(get_last)
  if [ -n "$last" ]; then
    read -rp "  Reutilizar o último pedido ($last)? [S/n]: " reuse
    case "$reuse" in n|N) pid=$(uuid);; *) pid="$last";; esac
  else
    pid=$(uuid)
  fi
  save_last "$pid"
  read -rp "  Cidade [Sao Paulo]: " cid;        cid=${cid:-Sao Paulo}
  code=$(PF_EID="$(uuid)" PF_PID="$pid" PF_CLI="$(uuid)" PF_CID="$cid" python -c "
import os,json
print(json.dumps({'eventId':os.environ['PF_EID'],'pedidoId':os.environ['PF_PID'],
  'clienteId':os.environ['PF_CLI'],'ocorridoEm':'2026-06-23T11:00:00Z',
  'endereco':{'logradouro':'Rua das Flores','numero':'100','complemento':'Apto 12',
              'bairro':'Centro','cidade':os.environ['PF_CID'],'uf':'SP','cep':'01000-000'}}))
" | post_event /pedidos/despachar)
  echo -e "  → publicado (HTTP ${C}$code${N}) pedidoId=${C}$pid${N}"
  echo -e "  ${D}aguardando o Transporte consumir...${N}"; sleep 2
  echo -e "  entrega criada:"
  curl -s "$TRANSP/entregas/$pid" | pretty | sed 's/^/    /'
}

ver_estoque()  { echo -e "${B}Estoque${N}";  curl -s "$ALMOX/estoque"  | pretty | sed 's/^/  /'; }
ver_entregas() { echo -e "${B}Entregas${N}"; curl -s "$TRANSP/entregas" | pretty | sed 's/^/  /'; }

ver_filas() {
  echo -e "${B}Filas (RabbitMQ)${N}"
  curl -s -u "$RUSER:$RPASS" "$RABBIT/api/queues" | python -c "
import sys,json
for q in json.load(sys.stdin):
    r=q.get('messages_ready',0); u=q.get('messages_unacknowledged',0)
    flag='  <-- mensagens presas!' if (r and q['name'].endswith('dlq')) else ''
    print(f\"  {q['name']:45} prontas={r} processando={u}{flag}\")
"
}

testar_dlq() {
  echo -e "${B}Teste de DLQ${N} ${D}(SKU inexistente -> 3 retentativas -> dead-letter)${N}"
  pid=$(uuid)
  body=$(printf '{"eventId":"%s","pedidoId":"%s","ocorridoEm":"2026-06-23T10:00:00Z","itens":[{"sku":"XPTO-999","quantidade":1}]}' "$(uuid)" "$pid")
  curl -s -o /dev/null -X POST "$PEDIDOS/pedidos/confirmar" -H "Content-Type: application/json" -d "$body"
  echo -e "  → publicado SKU inexistente. ${D}aguardando retentativas (~7s)...${N}"; sleep 8
  ver_filas
}

menu() {
  clear
  echo -e "${C}${B}"
  echo "  ┌──────────────────────────────────────────────┐"
  echo "  │            PetFriends — Console               │"
  echo "  └──────────────────────────────────────────────┘"
  echo -e "${N}"
  echo -e "  ${B}1${N}) Confirmar pedido   ${D}(reserva estoque)${N}"
  echo -e "  ${B}2${N}) Despachar pedido   ${D}(cria entrega)${N}"
  echo -e "  ${B}3${N}) Ver estoque"
  echo -e "  ${B}4${N}) Ver entregas"
  echo -e "  ${B}5${N}) Ver filas (RabbitMQ)"
  echo -e "  ${B}6${N}) Testar DLQ        ${D}(SKU inexistente)${N}"
  echo -e "  ${B}s${N}) Status dos serviços"
  echo -e "  ${B}0${N}) Sair"
  echo
}

while true; do
  menu
  read -rp "  Escolha: " opt
  echo
  case "$opt" in
    1) confirmar; pause;;
    2) despachar; pause;;
    3) ver_estoque; pause;;
    4) ver_entregas; pause;;
    5) ver_filas; pause;;
    6) testar_dlq; pause;;
    s|S) check; pause;;
    0) echo -e "  ${G}Até mais!${N}"; exit 0;;
    *) echo -e "  ${R}Opção inválida.${N}"; sleep 1;;
  esac
done
