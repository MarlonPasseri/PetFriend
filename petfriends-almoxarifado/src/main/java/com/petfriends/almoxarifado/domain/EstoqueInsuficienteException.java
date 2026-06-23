package com.petfriends.almoxarifado.domain;

public class EstoqueInsuficienteException extends RuntimeException {
    public EstoqueInsuficienteException(SKU sku, Quantidade solicitada, Quantidade disponivel) {
        super("Estoque insuficiente para SKU " + sku.valor()
                + ": solicitado " + solicitada + ", disponível " + disponivel);
    }
}
