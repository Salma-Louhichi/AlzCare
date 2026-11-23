package com.alzcare.notification.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient configuration for the notification service.
 * <p>Provides a load-balanced WebClient bean for making HTTP requests to other microservices,
 * particularly the Identity Service for user information retrieval.</p>
 */
@Configuration
public class WebClientConfig {

    /**
     * Creates a load-balanced WebClient bean.
     * <p>The @LoadBalanced annotation enables client-side load balancing through Eureka
     * service discovery, allowing the WebClient to resolve service names to actual instances.</p>
     *
     * @return a configured WebClient.Builder instance
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Creates a WebClient bean from the builder.
     * <p>This bean is injected into services that need to make HTTP requests to other microservices.</p>
     *
     * @param builder the load-balanced WebClient.Builder
     * @return a configured WebClient instance
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}
