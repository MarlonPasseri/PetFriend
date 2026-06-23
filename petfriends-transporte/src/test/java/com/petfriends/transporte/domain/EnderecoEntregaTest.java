package com.petfriends.transporte.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnderecoEntregaTest {

    private EnderecoEntrega valido(String uf, String cep) {
        return new EnderecoEntrega("Rua A", "100", "Apto 1", "Centro", "Sao Paulo", uf, cep);
    }

    @Test
    void aceitaEnderecoValido() {
        EnderecoEntrega e = valido("SP", "01000-000");
        assertThat(e.cidade()).isEqualTo("Sao Paulo");
        assertThat(e.uf()).isEqualTo("SP");
    }

    @Test
    void rejeitaUfInvalida() {
        assertThatThrownBy(() -> valido("SAO", "01000-000"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaCepInvalido() {
        assertThatThrownBy(() -> valido("SP", "123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void comparaPorValor() {
        assertThat(valido("SP", "01000-000")).isEqualTo(valido("SP", "01000-000"));
    }
}
