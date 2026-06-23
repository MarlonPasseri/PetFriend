package com.petfriends.shared.events;

/**
 * Fonte única da verdade para os nomes de exchange e routing keys usados na
 * comunicação assíncrona entre os microsserviços. Evita strings mágicas
 * duplicadas (um erro de digitação quebraria o roteamento silenciosamente).
 */
public final class EventRouting {

    private EventRouting() {
    }

    /** Topic exchange por onde trafegam os eventos do agregado Pedido. */
    public static final String EXCHANGE = "pedidos.exchange";

    /** Routing key do evento de pedido confirmado (consumido pelo Almoxarifado). */
    public static final String RK_PEDIDO_CONFIRMADO = "pedido.confirmado";

    /** Routing key do evento de pedido despachado (consumido pelo Transporte). */
    public static final String RK_PEDIDO_DESPACHADO = "pedido.despachado";
}
