package com.petfriends.almoxarifado.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Modelo de persistência do registro de idempotência (eventos já processados). */
@Entity
@Table(name = "evento_processado")
public class EventoProcessadoJpaEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "processado_em", nullable = false)
    private Instant processadoEm;

    protected EventoProcessadoJpaEntity() {
    }

    public EventoProcessadoJpaEntity(UUID eventId, Instant processadoEm) {
        this.eventId = eventId;
        this.processadoEm = processadoEm;
    }

    public UUID getEventId() { return eventId; }
    public Instant getProcessadoEm() { return processadoEm; }
}
