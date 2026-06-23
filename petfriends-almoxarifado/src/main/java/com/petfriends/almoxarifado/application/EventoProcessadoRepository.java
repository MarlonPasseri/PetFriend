package com.petfriends.almoxarifado.application;

import java.util.UUID;

/**
 * Porta de saída para o controle de idempotência do consumo de eventos.
 *
 * Como a entrega do RabbitMQ é at-least-once (e há retry configurado), o mesmo
 * evento pode ser entregue mais de uma vez. Registrar o eventId já processado
 * permite descartar reprocessamentos e evitar efeitos colaterais duplicados
 * (ex.: reservar estoque duas vezes).
 */
public interface EventoProcessadoRepository {

    /** Indica se o evento com este id já foi processado anteriormente. */
    boolean jaProcessado(UUID eventId);

    /** Marca o evento como processado (deve participar da mesma transação). */
    void registrar(UUID eventId);
}
