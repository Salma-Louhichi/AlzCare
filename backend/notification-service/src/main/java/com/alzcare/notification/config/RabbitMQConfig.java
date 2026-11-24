package com.alzcare.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ configuration for notification service.
 * <p>Configures exchanges, queues, and bindings for consuming notification events
 * from the safety-alert-engine with dead letter exchange (DLX) support.</p>
 */
@Configuration
public class RabbitMQConfig {

    // Exchange names
    @Value("${rabbitmq.exchange.notification:alert.notifications}")
    private String notificationExchangeName;

    @Value("${rabbitmq.exchange.notification-dlx:alert.notifications.dlx}")
    private String deadLetterExchangeName;

    // Queue names
    @Value("${rabbitmq.queue.notification:notification.queue}")
    private String notificationQueueName;

    @Value("${rabbitmq.queue.notification-dlq:notification.queue.dlq}")
    private String deadLetterQueueName;

    // Routing keys
    @Value("${rabbitmq.routing.key:notification.routing.key}")
    private String notificationRoutingKey;

    @Value("${rabbitmq.routing.dlq-key:notification.dlq.key}")
    private String deadLetterRoutingKey;

    // Queue configuration constants
    private static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    private static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
    private static final String X_MESSAGE_TTL = "x-message-ttl";
    private static final int MESSAGE_TTL_MS = 30000; // 30 seconds

    /**
     * Main notification exchange for receiving alert events.
     */
    /**
     * JSON message format for events exchanged with other services.
     *
     * <p>Without this bean Spring AMQP falls back to SimpleMessageConverter, which only handles
     * String, byte[] and java.io.Serializable payloads. NotificationEvent is none of these, so no
     * producer could deliver an event to {@code NotificationEventListener}. Spring Boot applies this
     * converter to both RabbitTemplate and the listener containers.</p>
     *
     * <p>The target type is always inferred from the listener method parameter, so producers
     * don't need to share Java class names through the {@code __TypeId__} header.</p>
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(notificationExchangeName, true, false);
    }

    /**
     * Main notification queue with DLX configuration.
     * <p>Messages that fail processing or expire will be routed to the DLX.</p>
     */
    @Bean
    public Queue notificationQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put(X_DEAD_LETTER_EXCHANGE, deadLetterExchangeName);
        args.put(X_DEAD_LETTER_ROUTING_KEY, deadLetterRoutingKey);
        args.put(X_MESSAGE_TTL, MESSAGE_TTL_MS);
        
        return QueueBuilder.durable(notificationQueueName)
                .withArguments(args)
                .build();
    }

    /**
     * Binding between notification queue and exchange.
     */
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(notificationExchange())
                .with(notificationRoutingKey);
    }

    /**
     * Dead Letter Exchange (DLX) for failed messages.
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(deadLetterExchangeName, true, false);
    }

    /**
     * Dead Letter Queue (DLQ) for storing failed messages.
     * <p>Messages that cannot be processed after retries will end up here
     * for manual investigation and reprocessing.</p>
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueueName).build();
    }

    /**
     * Binding between dead letter queue and dead letter exchange.
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(deadLetterRoutingKey);
    }
}
