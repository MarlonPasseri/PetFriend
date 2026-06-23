package com.petfriends.almoxarifado.domain;

import java.util.Objects;

/**
 * Value Object: identificador de produto no estoque.
 * Imutável e comparado por valor; valida o formato na construção.
 */
public record SKU(String valor) {
    public SKU {
        Objects.requireNonNull(valor, "SKU obrigatório");
        if (!valor.matches("[A-Z0-9\\-]{6,20}")) {
            throw new IllegalArgumentException("SKU inválido: " + valor);
        }
    }
}
