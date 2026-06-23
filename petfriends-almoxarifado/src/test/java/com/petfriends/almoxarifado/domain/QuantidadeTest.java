package com.petfriends.almoxarifado.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantidadeTest {

    @Test
    void naoPermiteValorNegativo() {
        assertThatThrownBy(() -> new Quantidade(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void somaESubtraiRetornandoNovasInstancias() {
        Quantidade tres = new Quantidade(3);
        Quantidade cinco = new Quantidade(5);
        assertThat(tres.somar(cinco)).isEqualTo(new Quantidade(8));
        assertThat(cinco.subtrair(tres)).isEqualTo(new Quantidade(2));
    }

    @Test
    void comparaPorValor() {
        assertThat(new Quantidade(7)).isEqualTo(new Quantidade(7));
        assertThat(new Quantidade(7).maiorQue(new Quantidade(3))).isTrue();
        assertThat(new Quantidade(3).maiorQue(new Quantidade(7))).isFalse();
    }
}
