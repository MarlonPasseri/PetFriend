#!/usr/bin/env bash
# Console interativo do PetFriends — dispara eventos e inspeciona o resultado.
# Requer: curl e python (ambos já usados no projeto). Rode no Git Bash.

PEDIDOS="http://localhost:8080"
ALMOX="http://localhost:8081"
TRANSP="http://localhost:8082"
RABBIT="http://localhost:15672"
RUSER="guest"; RPASS="guest"

# ---- paleta (256 cores) ----
N="\033[0m"; B="\033[1m"; D="\033[2m"
PINK="\033[38;5;213m"; CYAN="\033[38;5;81m"; GOLD="\033[38;5;220m"
GREEN="\033[38;5;42m"; RED="\033[38;5;203m"; GREY="\033[38;5;245m"
BADGE="\033[48;5;213m\033[38;5;236m"   # fundo rosa, texto escuro (números do menu)
RULE="${PINK}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${N}"

# ---- helpers ----
uuid() { python -c "import uuid;print(uuid.uuid4())"; }
LASTFILE=".petfriends_last_pedido"
save_last() { echo "$1" > "$LASTFILE"; }
get_last()  { cat "$LASTFILE" 2>/dev/null; }
pause() { echo; read -rp "$(echo -e "${GREY}  ↵  Enter para voltar ao menu...${N}")" _; }

# POST lendo o corpo JSON do stdin (ASCII puro do python json.dumps → acentos não quebram).
post_event() {
  curl -s -o /dev/null -w "%{http_code}" -X POST "$PEDIDOS$1" \
    -H "Content-Type: application/json; charset=utf-8" --data-binary @-
}

# spinner enquanto espera o consumidor processar
wait_spin() {
  local msg="$1" secs="$2"; local frames='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'; local i=0
  local end=$(( $(date +%s) + secs ))
  while [ "$(date +%s)" -lt "$end" ]; do
    printf "\r  ${GREY}%s${N} ${PINK}%s${N} " "$msg" "${frames:i++%10:1}"
    sleep 0.12
  done
  printf "\r\033[K"
}

up() { curl -s -o /dev/null -m 1 "$1"; }                       # serviço REST no ar?
rabbit_up() { curl -s -o /dev/null -m 1 -u "$RUSER:$RPASS" "$RABBIT/api/overview"; }
dot() { if eval "$2"; then printf "${GREEN}●${N}"; else printf "${RED}○${N}"; fi; }

section() { echo; echo -e "  ${PINK}▌${N}${B} $1${N}"; echo -e "  ${D}$2${N}"; }

http_badge() { # colore o código HTTP
  case "$1" in 2*) echo -e "${GREEN}$1${N}";; 4*|5*) echo -e "${RED}$1${N}";; *) echo "$1";; esac
}

# ---- renderizadores (JSON → tabela) ----
render_estoque() {
  python -c "
import sys,json
try: d=json.load(sys.stdin)
except Exception: print('  (sem resposta)'); sys.exit()
if isinstance(d,dict): d=[d]
rows=[x for x in d if 'sku' in x]
if not rows: print('  \033[2m(estoque vazio)\033[0m'); sys.exit()
print('  \033[1m{:<14}{:>12}{:>12}\033[0m'.format('SKU','Disponível','Reservada'))
print('  \033[38;5;245m'+'─'*38+'\033[0m')
for x in rows:
    print('  {:<14}{:>12}{:>12}'.format(x['sku'], x['quantidadeDisponivel'], x['quantidadeReservada']))
"
}

render_entregas() {
  python -c "
import sys,json
C={'EM_TRANSITO':'\033[38;5;220m','ENTREGUE':'\033[38;5;42m','EXTRAVIADA':'\033[38;5;203m','DEVOLVIDA':'\033[38;5;203m','CRIADA':'\033[38;5;245m'}
try: d=json.load(sys.stdin)
except Exception: print('  (sem resposta)'); sys.exit()
if isinstance(d,dict): d=[d]
rows=[x for x in d if 'pedidoId' in x]
if not rows: print('  \033[2m(nenhuma entrega)\033[0m'); sys.exit()
print('  \033[1m{:<12}{:<18}{}\033[0m'.format('Pedido','Cidade','Status'))
print('  \033[38;5;245m'+'─'*44+'\033[0m')
for x in rows:
    st=x['status']; cor=C.get(st,'')
    print('  {:<12}{:<18}{}{}\033[0m'.format(x['pedidoId'][:8]+'…', x['endereco']['cidade'], cor, st))
"
}

render_filas() {
  curl -s -u "$RUSER:$RPASS" "$RABBIT/api/queues" | python -c "
import sys,json
try: data=json.load(sys.stdin)
except Exception: print('  (RabbitMQ indisponível)'); sys.exit()
print('  \033[1m{:<46}{:>9}{:>7}\033[0m'.format('Fila','Prontas','Proc.'))
print('  \033[38;5;245m'+'─'*62+'\033[0m')
for q in sorted(data,key=lambda x:x['name']):
    r=q.get('messages_ready',0); u=q.get('messages_unacknowledged',0)
    nome=q['name']
    if nome.endswith('dlq') and r:
        print('  \033[38;5;203m{:<46}{:>9}{:>7}  ← presas!\033[0m'.format(nome,r,u))
    else:
        print('  {:<46}{:>9}{:>7}'.format(nome,r,u))
"
}

# ---- ações ----
confirmar() {
  section "Confirmar pedido" "reserva estoque no Almoxarifado"
  read -rp "  SKU [RACAO-001]: " sku;  sku=${sku:-RACAO-001}
  read -rp "  Quantidade [2]: " qtd;    qtd=${qtd:-2}
  pid=$(uuid); save_last "$pid"
  code=$(PF_EID="$(uuid)" PF_PID="$pid" PF_SKU="$sku" PF_QTD="$qtd" python -c "
import os,json
print(json.dumps({'eventId':os.environ['PF_EID'],'pedidoId':os.environ['PF_PID'],
  'ocorridoEm':'2026-06-23T10:00:00Z',
  'itens':[{'sku':os.environ['PF_SKU'],'quantidade':int(os.environ['PF_QTD'])}]}))
" | post_event /pedidos/confirmar)
  echo -e "  publicado  ${D}HTTP${N} $(http_badge "$code")   ${D}pedido${N} ${CYAN}${pid:0:8}…${N}"
  wait_spin "Almoxarifado consumindo o evento" 3
  echo -e "  ${GREEN}✓${N} estoque atualizado:"; echo
  curl -s "$ALMOX/estoque/$sku" | render_estoque
}

despachar() {
  section "Despachar pedido" "cria a entrega no Transporte"
  last=$(get_last)
  if [ -n "$last" ]; then
    read -rp "  Reutilizar o último pedido (${last:0:8}…)? [S/n]: " reuse
    case "$reuse" in n|N) pid=$(uuid);; *) pid="$last";; esac
  else
    pid=$(uuid)
  fi
  save_last "$pid"
  read -rp "  Cidade [São Paulo]: " cid;  cid=${cid:-São Paulo}
  code=$(PF_EID="$(uuid)" PF_PID="$pid" PF_CLI="$(uuid)" PF_CID="$cid" python -c "
import os,json
print(json.dumps({'eventId':os.environ['PF_EID'],'pedidoId':os.environ['PF_PID'],
  'clienteId':os.environ['PF_CLI'],'ocorridoEm':'2026-06-23T11:00:00Z',
  'endereco':{'logradouro':'Rua das Flores','numero':'100','complemento':'Apto 12',
              'bairro':'Centro','cidade':os.environ['PF_CID'],'uf':'SP','cep':'01000-000'}}))
" | post_event /pedidos/despachar)
  echo -e "  publicado  ${D}HTTP${N} $(http_badge "$code")   ${D}pedido${N} ${CYAN}${pid:0:8}…${N}"
  wait_spin "Transporte consumindo o evento" 3
  echo -e "  ${GREEN}✓${N} entrega criada:"; echo
  curl -s "$TRANSP/entregas/$pid" | render_entregas
}

ver_estoque()  { section "Estoque" "Almoxarifado"; echo; curl -s "$ALMOX/estoque"  | render_estoque; }
ver_entregas() { section "Entregas" "Transporte";  echo; curl -s "$TRANSP/entregas" | render_entregas; }
ver_filas()    { section "Filas" "RabbitMQ — trabalho + dead-letter"; echo; render_filas; }

testar_dlq() {
  section "Teste de DLQ" "SKU inexistente → 3 retentativas → dead-letter"
  pid=$(uuid)
  PF_EID="$(uuid)" PF_PID="$pid" python -c "
import os,json
print(json.dumps({'eventId':os.environ['PF_EID'],'pedidoId':os.environ['PF_PID'],
  'ocorridoEm':'2026-06-23T10:00:00Z','itens':[{'sku':'XPTO-999','quantidade':1}]}))
" | post_event /pedidos/confirmar >/dev/null
  echo -e "  publicado SKU inexistente ${D}(XPTO-999)${N}"
  wait_spin "aguardando as retentativas" 8
  echo; render_filas
}

status_full() {
  section "Status dos serviços" "checagem ao vivo"
  echo
  echo -e "  $(dot _ 'up $PEDIDOS/pedidos')  Pedidos        ${D}$PEDIDOS${N}"
  echo -e "  $(dot _ 'up $ALMOX/estoque')  Almoxarifado   ${D}$ALMOX${N}"
  echo -e "  $(dot _ 'up $TRANSP/entregas')  Transporte     ${D}$TRANSP${N}"
  echo -e "  $(dot _ 'rabbit_up')  RabbitMQ       ${D}$RABBIT${N}"
}

# ---- menu ----
menu() {
  clear
  echo
  echo -e "$RULE"
  echo -e "   ${PINK}${B}🐾  PetFriends${N}  ${GREY}·${N}  ${B}Console de Microsserviços${N}"
  echo -e "$RULE"
  printf "   %b Pedidos   %b Almoxarifado   %b Transporte   %b RabbitMQ\n" \
    "$(dot _ 'up $PEDIDOS/pedidos')" "$(dot _ 'up $ALMOX/estoque')" \
    "$(dot _ 'up $TRANSP/entregas')" "$(dot _ 'rabbit_up')"
  echo
  echo -e "   ${BADGE} 1 ${N}  Confirmar pedido     ${GREY}reserva estoque${N}"
  echo -e "   ${BADGE} 2 ${N}  Despachar pedido     ${GREY}cria entrega${N}"
  echo -e "   ${BADGE} 3 ${N}  Ver estoque"
  echo -e "   ${BADGE} 4 ${N}  Ver entregas"
  echo -e "   ${BADGE} 5 ${N}  Ver filas            ${GREY}RabbitMQ + DLQ${N}"
  echo -e "   ${BADGE} 6 ${N}  Testar DLQ           ${GREY}SKU inexistente${N}"
  echo -e "   ${BADGE} s ${N}  Status dos serviços"
  echo -e "   ${BADGE} 0 ${N}  Sair"
  echo
}

while true; do
  menu
  read -rp "$(echo -e "   ${PINK}❯${N} escolha: ")" opt
  case "$opt" in
    1) confirmar; pause;;
    2) despachar; pause;;
    3) ver_estoque; pause;;
    4) ver_entregas; pause;;
    5) ver_filas; pause;;
    6) testar_dlq; pause;;
    s|S) status_full; pause;;
    0) echo; echo -e "   ${PINK}🐾 Até mais!${N}"; echo; exit 0;;
    *) echo -e "   ${RED}opção inválida${N}"; sleep 1;;
  esac
done
