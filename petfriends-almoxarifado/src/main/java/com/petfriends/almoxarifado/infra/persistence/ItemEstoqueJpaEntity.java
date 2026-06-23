package com.petfriends.almoxarifado.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Modelo de persistência (tabela), separado do modelo de domínio. */
@Entity
@Table(name = "item_estoque")
public class ItemEstoqueJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(name = "quantidade_disponivel", nullable = false)
    private int quantidadeDisponivel;

    @Column(name = "quantidade_reservada", nullable = false)
    private int quantidadeReservada;

    protected ItemEstoqueJpaEntity() {
    }

    public ItemEstoqueJpaEntity(UUID id, String sku, int quantidadeDisponivel, int quantidadeReservada) {
        this.id = id;
        this.sku = sku;
        this.quantidadeDisponivel = quantidadeDisponivel;
        this.quantidadeReservada = quantidadeReservada;
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public int getQuantidadeDisponivel() { return quantidadeDisponivel; }
    public int getQuantidadeReservada() { return quantidadeReservada; }
}
