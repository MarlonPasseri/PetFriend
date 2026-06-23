package com.petfriends.transporte.domain;

import com.petfriends.transporte.domain.Entrega.StatusEntrega;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntregaTest {

    private Entrega nova() {
        EnderecoEntrega end = new EnderecoEntrega("Rua A", "100", "", "Centro", "Sao Paulo", "SP", "01000-000");
        return new Entrega(UUID.randomUUID(), UUID.randomUUID(), end);
    }

    @Test
    void nasceCriada() {
        assertThat(nova().getStatus()).isEqualTo(StatusEntrega.CRIADA);
    }

    @Test
    void despacharColocaEmTransito() {
        Entrega e = nova();
        e.despachar();
        assertThat(e.getStatus()).isEqualTo(StatusEntrega.EM_TRANSITO);
        assertThat(e.getDespachadaEm()).isNotNull();
    }

    @Test
    void confirmarEntregaSoAposTransito() {
        Entrega e = nova();
        assertThatThrownBy(e::confirmarEntrega).isInstanceOf(IllegalStateException.class);
        e.despachar();
        e.confirmarEntrega();
        assertThat(e.getStatus()).isEqualTo(StatusEntrega.ENTREGUE);
    }

    @Test
    void marcarExtraviadaSoAposTransito() {
        Entrega e = nova();
        assertThatThrownBy(e::marcarExtraviada).isInstanceOf(IllegalStateException.class);
        e.despachar();
        e.marcarExtraviada();
        assertThat(e.getStatus()).isEqualTo(StatusEntrega.EXTRAVIADA);
    }

    @Test
    void naoDespachaDuasVezes() {
        Entrega e = nova();
        e.despachar();
        assertThatThrownBy(e::despachar).isInstanceOf(IllegalStateException.class);
    }
}
