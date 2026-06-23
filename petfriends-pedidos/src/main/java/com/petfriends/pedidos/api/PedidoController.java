package com.petfriends.pedidos.api;

import com.petfriends.pedidos.application.PedidoEventPublisher;
import com.petfriends.shared.events.PedidoConfirmadoEvent;
import com.petfriends.shared.events.PedidoDespachadoEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de demonstração para disparar o fluxo event-driven.
 * Em produção, estes eventos seriam emitidos como efeito das transições de
 * estado do agregado Pedido (confirmação de pagamento, despacho), não por REST.
 */
@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoEventPublisher publisher;

    public PedidoController(PedidoEventPublisher publisher) {
        this.publisher = publisher;
    }

    /** Simula a confirmação de pagamento -> notifica o Almoxarifado. */
    @PostMapping("/confirmar")
    public ResponseEntity<Void> confirmar(@RequestBody PedidoConfirmadoEvent evento) {
        publisher.publicarPedidoConfirmado(evento);
        return ResponseEntity.accepted().build();
    }

    /** Simula o despacho do pedido -> notifica o Transporte. */
    @PostMapping("/despachar")
    public ResponseEntity<Void> despachar(@RequestBody PedidoDespachadoEvent evento) {
        publisher.publicarPedidoDespachado(evento);
        return ResponseEntity.accepted().build();
    }
}
