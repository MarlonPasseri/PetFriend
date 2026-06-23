package com.petfriends.almoxarifado.domain;

/**
 * Value Object: quantidade não-negativa de itens em estoque.
 * Imutável; as operações retornam novas instâncias.
 */
public final class Quantidade {

    private final int valor;

    public Quantidade(int valor) {
        if (valor < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa");
        }
        this.valor = valor;
    }

    public Quantidade somar(Quantidade outra) {
        return new Quantidade(this.valor + outra.valor);
    }

    public Quantidade subtrair(Quantidade outra) {
        return new Quantidade(this.valor - outra.valor);
    }

    public boolean maiorQue(Quantidade outra) {
        return this.valor > outra.valor;
    }

    public int valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quantidade outra)) return false;
        return valor == outra.valor;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(valor);
    }

    @Override
    public String toString() {
        return String.valueOf(valor);
    }
}
