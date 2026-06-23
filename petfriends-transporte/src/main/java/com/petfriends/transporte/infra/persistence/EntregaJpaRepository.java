package com.petfriends.transporte.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EntregaJpaRepository extends JpaRepository<EntregaJpaEntity, UUID> {
    Optional<EntregaJpaEntity> findByPedidoId(UUID pedidoId);
}
