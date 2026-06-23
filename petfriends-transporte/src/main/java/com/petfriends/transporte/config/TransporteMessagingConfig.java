package com.petfriends.transporte.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de mensageria do PetFriends_Transporte.
 *
 * Declara a exchange (compartilhada com Pedidos), a fila própria do serviço,
 * o binding pela routing key dos eventos de pedido despachado e uma DLQ.
 */
@Configuration
public class TransporteMessagingConfig {

    public static final String EXCHANGE = "pedidos.exchange";
    public static final String QUEUE = "transporte.pedido-despachado.queue";
    public static final String DLQ = "transporte.pedido-despachado.dlq";
    public static final String ROUTING_KEY = "pedido.despachado";

    @Bean
    TopicExchange pedidosExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    Queue transporteDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Queue transporteQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    Binding transporteBinding(Queue transporteQueue, TopicExchange pedidosExchange) {
        return BindingBuilder.bind(transporteQueue)
                .to(pedidosExchange)
                .with(ROUTING_KEY);
    }

    /** Converte o corpo JSON da mensagem no record PedidoDespachadoEvent. */
    @Bean
    MessageConverter jacksonConverter() {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        return new Jackson2JsonMessageConverter(mapper);
    }
}
