package com.petfriends.almoxarifado.config;

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
 * Configuração de mensageria do PetFriends_Almoxarifado.
 *
 * Declara a exchange (compartilhada com Pedidos), a fila própria do serviço,
 * o binding pela routing key dos eventos de pedido confirmado e uma DLQ para
 * mensagens que falharem após as tentativas de reprocessamento.
 */
@Configuration
public class AlmoxarifadoMessagingConfig {

    public static final String EXCHANGE = "pedidos.exchange";
    public static final String QUEUE = "almoxarifado.pedido-confirmado.queue";
    public static final String DLQ = "almoxarifado.pedido-confirmado.dlq";
    public static final String ROUTING_KEY = "pedido.confirmado";

    @Bean
    TopicExchange pedidosExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    Queue almoxarifadoDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Queue almoxarifadoQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    Binding almoxarifadoBinding(Queue almoxarifadoQueue, TopicExchange pedidosExchange) {
        return BindingBuilder.bind(almoxarifadoQueue)
                .to(pedidosExchange)
                .with(ROUTING_KEY);
    }

    /** Converte o corpo JSON da mensagem no record PedidoConfirmadoEvent. */
    @Bean
    MessageConverter jacksonConverter() {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        return new Jackson2JsonMessageConverter(mapper);
    }
}
