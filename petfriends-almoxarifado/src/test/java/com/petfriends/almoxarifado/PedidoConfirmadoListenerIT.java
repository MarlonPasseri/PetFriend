package com.petfriends.almoxarifado;

import com.petfriends.almoxarifado.domain.ItemEstoqueRepository;
import com.petfriends.almoxarifado.domain.SKU;
import com.petfriends.shared.events.EventRouting;
import com.petfriends.shared.events.PedidoConfirmadoEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Teste de integração com RabbitMQ real (Testcontainers): publica um
 * PedidoConfirmadoEvent na exchange e verifica que o listener consumiu e
 * reservou o estoque do SKU semeado pelo Flyway (RACAO-001 = 100).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)   // pula se o Docker não estiver acessível
class PedidoConfirmadoListenerIT {

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer("rabbitmq:3.13-management");

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    ItemEstoqueRepository repository;

    @Test
    void reservaEstoqueAoReceberPedidoConfirmado() {
        var evento = new PedidoConfirmadoEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                List.of(new PedidoConfirmadoEvent.ItemPedido("RACAO-001", 3)));

        rabbitTemplate.convertAndSend(EventRouting.EXCHANGE, EventRouting.RK_PEDIDO_CONFIRMADO, evento);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var item = repository.buscarPorSku(new SKU("RACAO-001")).orElseThrow();
            assertThat(item.getQuantidadeReservada().valor()).isEqualTo(3);
            assertThat(item.getQuantidadeDisponivel().valor()).isEqualTo(97);
        });
    }

    @Test
    void naoReservaEmDuplicidadeQuandoOMesmoEventoEhReentregue() {
        // Estado atual antes do teste (independe de ordem com outros testes,
        // pois o H2 em memória é compartilhado no mesmo JVM).
        int reservadoInicial = repository.buscarPorSku(new SKU("RACAO-001"))
                .orElseThrow().getQuantidadeReservada().valor();

        // Mesmo eventId entregue duas vezes (simula a reentrega at-least-once).
        var evento = new PedidoConfirmadoEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                List.of(new PedidoConfirmadoEvent.ItemPedido("RACAO-001", 5)));

        rabbitTemplate.convertAndSend(EventRouting.EXCHANGE, EventRouting.RK_PEDIDO_CONFIRMADO, evento);

        // Aguarda a primeira reserva ser aplicada (+5).
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var item = repository.buscarPorSku(new SKU("RACAO-001")).orElseThrow();
            assertThat(item.getQuantidadeReservada().valor()).isEqualTo(reservadoInicial + 5);
        });

        // Reentrega do MESMO evento: deve ser descartada pela idempotência.
        rabbitTemplate.convertAndSend(EventRouting.EXCHANGE, EventRouting.RK_PEDIDO_CONFIRMADO, evento);

        // Mesmo após tempo suficiente, a reserva NÃO é aplicada de novo (+5 apenas).
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var item = repository.buscarPorSku(new SKU("RACAO-001")).orElseThrow();
            assertThat(item.getQuantidadeReservada().valor()).isEqualTo(reservadoInicial + 5);
        });
    }
}
