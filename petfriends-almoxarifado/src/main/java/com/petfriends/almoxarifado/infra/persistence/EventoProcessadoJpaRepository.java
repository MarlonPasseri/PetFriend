package com.petfriends.almoxarifado.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoProcessadoJpaRepository extends JpaRepository<EventoProcessadoJpaEntity, UUID> {
}
