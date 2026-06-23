package com.petfriends.almoxarifado.domain;

public class ItemEstoqueNaoEncontradoException extends RuntimeException {
    public ItemEstoqueNaoEncontradoException(SKU sku) {
        super("Item de estoque não encontrado para SKU " + sku.valor());
    }
}
