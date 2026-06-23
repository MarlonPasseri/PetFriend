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

    public PedidoConfirmadoListener(ItemEstoqueRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = AlmoxarifadoMessagingConfig.QUEUE)
    @Transactional
    public void onPedidoConfirmado(PedidoConfirmadoEvent evento) {
        log.info("Recebido PedidoConfirmadoEvent pedidoId={} eventId={}",
                evento.pedidoId(), evento.eventId());

        for (PedidoConfirmadoEvent.ItemPedido item : evento.itens()) {
            SKU sku = new SKU(item.sku());
            ItemEstoque estoque = repository.buscarPorSku(sku)
                    .orElseThrow(() -> new ItemEstoqueNaoEncontradoException(sku));

            estoque.reservar(new Quantidade(item.quantidade()));
            repository.salvar(estoque);

            log.info("Estoque reservado SKU={} quantidade={}", item.sku(), item.quantidade());
        }
        // Recomendado: registrar eventId já processado para garantir idempotência
        // em entregas at-least-once (descartar reprocessamentos).
    }
}
