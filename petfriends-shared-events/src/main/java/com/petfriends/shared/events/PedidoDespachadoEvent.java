package com.petfriends.shared.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de domínio emitido por PetFriends_Pedidos quando o pedido é despachado
 * (estado "Em Preparação" -> "Em Trânsito" no diagrama de estados).
 *
 * Estratégia: event-carried state transfer. Carrega o endereço de entrega para
 * que o Transporte crie a Entrega sem precisar de callback síncrono.
 */
public record PedidoDespachadoEvent(
        UUID eventId,
        UUID pedidoId,
        UUID clienteId,
        Instant ocorridoEm,
        EnderecoEntregaDto endereco) {

    public record EnderecoEntregaDto(
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String uf,
            String cep) {}
}
