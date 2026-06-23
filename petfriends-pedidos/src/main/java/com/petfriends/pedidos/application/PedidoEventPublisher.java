package com.petfriends.pedidos.application;

import com.petfriends.pedidos.config.PedidosMessagingConfig;
import com.petfriends.shared.events.PedidoConfirmadoEvent;
import com.petfriends.shared.events.PedidoDespachadoEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/** Publica os eventos de domínio do agregado Pedido na exchange. */
@Service
public class PedidoEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PedidoEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarPedidoConfirmado(PedidoConfirmadoEvent evento) {
        rabbitTemplate.convertAndSend(
                PedidosMessagingConfig.EXCHANGE,
                PedidosMessagingConfig.RK_CONFIRMADO,
                evento);
    }

    public void publicarPedidoDespachado(PedidoDespachadoEvent evento) {
        rabbitTemplate.convertAndSend(
                PedidosMessagingConfig.EXCHANGE,
                PedidosMessagingConfig.RK_DESPACHADO,
                evento);
    }
}
