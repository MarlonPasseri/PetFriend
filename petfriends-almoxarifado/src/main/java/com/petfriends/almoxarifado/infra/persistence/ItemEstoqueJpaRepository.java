package com.petfriends.almoxarifado.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ItemEstoqueJpaRepository extends JpaRepository<ItemEstoqueJpaEntity, UUID> {
    Optional<ItemEstoqueJpaEntity> findBySku(String sku);
}
