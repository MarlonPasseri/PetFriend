package com.petfriends.almoxarifado.domain;

import java.util.UUID;

/**
 * Aggregate Root do microsserviço PetFriends_Almoxarifado.
 *
 * Representa a posição de estoque de um produto e protege o invariante central
 * do contexto: nunca reservar/baixar mais do que está disponível.
 */
public class ItemEstoque {

    private final UUID id;
    private final SKU sku;
    private Quantidade quantidadeDisponivel;
    private Quantidade quantidadeReservada;

    public ItemEstoque(UUID id, SKU sku,
                       Quantidade quantidadeDisponivel,
                       Quantidade quantidadeReservada) {
        this.id = id;
        this.sku = sku;
        this.quantidadeDisponivel = quantidadeDisponivel;
        this.quantidadeReservada = quantidadeReservada;
    }

    /** Reserva estoque para um pedido confirmado. */
    public void reservar(Quantidade qtd) {
        if (qtd.maiorQue(this.quantidadeDisponivel)) {
            throw new EstoqueInsuficienteException(sku, qtd, quantidadeDisponivel);
        }
        this.quantidadeDisponivel = this.quantidadeDisponivel.subtrair(qtd);
        this.quantidadeReservada = this.quantidadeReservada.somar(qtd);
    }

    /** Confirma a saída física do estoque (pedido despachado). */
    public void baixarReserva(Quantidade qtd) {
        this.quantidadeReservada = this.quantidadeReservada.subtrair(qtd);
    }

    /** Devolve a reserva ao disponível (pedido cancelado/devolvido). */
    public void liberarReserva(Quantidade qtd) {
        this.quantidadeReservada = this.quantidadeReservada.subtrair(qtd);
        this.quantidadeDisponivel = this.quantidadeDisponivel.somar(qtd);
    }

    public UUID getId() { return id; }
    public SKU getSku() { return sku; }
    public Quantidade getQuantidadeDisponivel() { return quantidadeDisponivel; }
    public Quantidade getQuantidadeReservada() { return quantidadeReservada; }
}
