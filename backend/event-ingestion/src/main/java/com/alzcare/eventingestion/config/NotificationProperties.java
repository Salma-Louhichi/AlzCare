package com.alzcare.eventingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alzcare.notifications")
public record NotificationProperties(String exchange, String routingKey) {
}
