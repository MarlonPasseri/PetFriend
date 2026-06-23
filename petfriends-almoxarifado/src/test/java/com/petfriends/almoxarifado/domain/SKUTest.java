package com.petfriends.almoxarifado.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SKUTest {

    @Test
    void aceitaFormatoValido() {
        assertThat(new SKU("RACAO-001").valor()).isEqualTo("RACAO-001");
    }

    @Test
    void rejeitaFormatoInvalido() {
        assertThatThrownBy(() -> new SKU("abc"))           // minúsculas e curto
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SKU(null))
                .isInstanceOf(NullPointerException.class);
    }
}
