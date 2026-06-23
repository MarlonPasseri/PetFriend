package com.petfriends.almoxarifado.infra.persistence;

import com.petfriends.almoxarifado.application.EventoProcessadoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/** Adaptador: implementa a porta de idempotência usando Spring Data JPA. */
@Repository
public class EventoProcessadoRepositoryJpa implements EventoProcessadoRepository {

    private final EventoProcessadoJpaRepository jpa;

    public EventoProcessadoRepositoryJpa(EventoProcessadoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean jaProcessado(UUID eventId) {
        return jpa.existsById(eventId);
    }

    @Override
    public void registrar(UUID eventId) {
        jpa.save(new EventoProcessadoJpaEntity(eventId, Instant.now()));
    }
}
