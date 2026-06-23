package com.petfriends.transporte.infra.persistence;

import com.petfriends.transporte.domain.Entrega.StatusEntrega;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "entrega")
public class EntregaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "pedido_id", nullable = false, unique = true)
    private UUID pedidoId;

    @Embedded
    private EnderecoEmbeddable endereco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEntrega status;

    @Column(name = "despachada_em")
    private LocalDateTime despachadaEm;

    @Column(name = "atualizada_em")
    private LocalDateTime atualizadaEm;

    protected EntregaJpaEntity() {
    }

    public EntregaJpaEntity(UUID id, UUID pedidoId, EnderecoEmbeddable endereco,
                            StatusEntrega status, LocalDateTime despachadaEm, LocalDateTime atualizadaEm) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.endereco = endereco;
        this.status = status;
        this.despachadaEm = despachadaEm;
        this.atualizadaEm = atualizadaEm;
    }

    public UUID getId() { return id; }
    public UUID getPedidoId() { return pedidoId; }
    public EnderecoEmbeddable getEndereco() { return endereco; }
    public StatusEntrega getStatus() { return status; }
    public LocalDateTime getDespachadaEm() { return despachadaEm; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
}
