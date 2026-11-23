package com.alzcare.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB configuration for the notification service.
 * <p>Enables MongoDB auditing for automatic population of creation and modification timestamps,
 * and configures the base package for MongoDB repositories.</p>
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.alzcare.notification.repositories")
public class MongoConfig {
    // Configuration is handled through annotations
}
