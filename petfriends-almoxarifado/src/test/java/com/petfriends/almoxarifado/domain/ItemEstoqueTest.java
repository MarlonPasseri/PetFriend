package com.petfriends.almoxarifado.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemEstoqueTest {

    private ItemEstoque comDisponivel(int disponivel, int reservada) {
        return new ItemEstoque(UUID.randomUUID(), new SKU("RACAO-001"),
                new Quantidade(disponivel), new Quantidade(reservada));
    }

    @Test
    void reservarReduzDisponivelEAumentaReservada() {
        ItemEstoque item = comDisponivel(10, 0);
        item.reservar(new Quantidade(3));
        assertThat(item.getQuantidadeDisponivel()).isEqualTo(new Quantidade(7));
        assertThat(item.getQuantidadeReservada()).isEqualTo(new Quantidade(3));
    }

    @Test
    void reservarAcimaDoDisponivelViolaInvarianteEFalha() {
        ItemEstoque item = comDisponivel(2, 0);
        assertThatThrownBy(() -> item.reservar(new Quantidade(5)))
                .isInstanceOf(EstoqueInsuficienteException.class);
        // estado permanece intacto após a falha
        assertThat(item.getQuantidadeDisponivel()).isEqualTo(new Quantidade(2));
        assertThat(item.getQuantidadeReservada()).isEqualTo(new Quantidade(0));
    }

    @Test
    void baixarReservaConsomeAReserva() {
        ItemEstoque item = comDisponivel(7, 3);
        item.baixarReserva(new Quantidade(3));
        assertThat(item.getQuantidadeReservada()).isEqualTo(new Quantidade(0));
        assertThat(item.getQuantidadeDisponivel()).isEqualTo(new Quantidade(7));
    }

    @Test
    void liberarReservaDevolveAoDisponivel() {
        ItemEstoque item = comDisponivel(7, 3);
        item.liberarReserva(new Quantidade(3));
        assertThat(item.getQuantidadeReservada()).isEqualTo(new Quantidade(0));
        assertThat(item.getQuantidadeDisponivel()).isEqualTo(new Quantidade(10));
    }
}
