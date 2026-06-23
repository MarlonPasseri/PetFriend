package com.petfriends.transporte.domain;

import java.util.Optional;
import java.util.UUID;

/** Repository do agregado Entrega (porta do domínio). */
public interface EntregaRepository {
    Optional<Entrega> buscarPorId(UUID id);
    Optional<Entrega> buscarPorPedido(UUID pedidoId);
    void salvar(Entrega entrega);
}
