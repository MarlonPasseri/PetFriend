package com.petfriends.shared.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Evento de domínio emitido por PetFriends_Pedidos quando um pedido tem o
 * pagamento confirmado (estado "Fechado" -> "Em Preparação" no diagrama de estados).
 *
 * Estratégia: event-carried state transfer. Carrega os itens (SKU + quantidade)
 * para que o Almoxarifado reserve estoque sem precisar de callback síncrono.
 */
public record PedidoConfirmadoEvent(
        UUID eventId,
        UUID pedidoId,
        Instant ocorridoEm,
        List<ItemPedido> itens) {

    public record ItemPedido(String sku, int quantidade) {}
}
