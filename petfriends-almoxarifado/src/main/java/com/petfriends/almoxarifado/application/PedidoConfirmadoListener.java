package com.petfriends.almoxarifado.application;

import com.petfriends.almoxarifado.config.AlmoxarifadoMessagingConfig;
import com.petfriends.almoxarifado.domain.ItemEstoque;
import com.petfriends.almoxarifado.domain.ItemEstoqueNaoEncontradoException;
import com.petfriends.almoxarifado.domain.ItemEstoqueRepository;
import com.petfriends.almoxarifado.domain.Quantidade;
import com.petfriends.almoxarifado.domain.SKU;
import com.petfriends.shared.events.PedidoConfirmadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço que recebe os eventos de PetFriends_Pedidos e reserva o estoque
 * correspondente a cada item do pedido confirmado.
 */
@Service
public class PedidoConfirmadoListener {

    private static final Logger log = LoggerFactory.getLogger(PedidoConfirmadoListener.class);

    private final ItemEstoqueRepository repository;
    private final EventoProcessadoRepository eventosProcessados;

    public PedidoConfirmadoListener(ItemEstoqueRepository repository,
                                    EventoProcessadoRepository eventosProcessados) {
        this.repository = repository;
        this.eventosProcessados = eventosProcessados;
    }

    @RabbitListener(queues = AlmoxarifadoMessagingConfig.QUEUE)
    @Transactional
    public void onPedidoConfirmado(PedidoConfirmadoEvent evento) {
        log.info("Recebido PedidoConfirmadoEvent pedidoId={} eventId={}",
                evento.pedidoId(), evento.eventId());

        // Idempotência: a entrega é at-least-once (com retry), então o mesmo
        // evento pode chegar mais de uma vez. Se já foi processado, descarta o
        // reprocessamento para não reservar estoque em duplicidade.
        if (eventosProcessados.jaProcessado(evento.eventId())) {
            log.info("Evento já processado eventId={}, ignorando (idempotência).", evento.eventId());
            return;
        }

        for (PedidoConfirmadoEvent.ItemPedido item : evento.itens()) {
            SKU sku = new SKU(item.sku());
            ItemEstoque estoque = repository.buscarPorSku(sku)
                    .orElseThrow(() -> new ItemEstoqueNaoEncontradoException(sku));

            estoque.reservar(new Quantidade(item.quantidade()));
            repository.salvar(estoque);

            log.info("Estoque reservado SKU={} quantidade={}", item.sku(), item.quantidade());
        }

        // Marca o evento como processado na MESMA transação da reserva: ou tudo
        // commita junto, ou nada (em falha, o retry/redelivery reprocessa do zero).
        // A PK em event_id também barra duplicatas concorrentes no commit.
        eventosProcessados.registrar(evento.eventId());
    }
}
