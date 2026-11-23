package com.alzcare.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notification Service Application
 * 
 * This service manages notifications for the AlzCare platform including:
 * - In-app notifications
 * - Email notifications
 * - SMS notifications
 * - Push notifications
 * 
 * It integrates with:
 * - MongoDB for notification storage
 * - RabbitMQ for message queuing
 * - Eureka for service discovery
 * - Config Server for centralized configuration
 * 
 * @author AlzCare Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.alzcare.notification.client")
@EnableScheduling
public class NotificationServiceApplication {

    /**
     * Main entry point for the Notification Service application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
