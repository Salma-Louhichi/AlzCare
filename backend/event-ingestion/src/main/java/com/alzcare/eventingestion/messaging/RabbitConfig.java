package com.alzcare.eventingestion.messaging;

import com.alzcare.eventingestion.config.NotificationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /** JSON, like notification-service expects (it infers the type from its listener). */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /** Same declaration as notification-service, so either service can start first. */
    @Bean
    public DirectExchange notificationExchange(NotificationProperties props) {
        return new DirectExchange(props.exchange(), true, false);
    }
}
