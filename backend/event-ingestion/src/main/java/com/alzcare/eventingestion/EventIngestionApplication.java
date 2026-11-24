package com.alzcare.eventingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Receives motion events from ESP32 home cameras, detects risky patterns
 * (wandering, night motion, night exit attempts) and alerts the care team.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
public class EventIngestionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventIngestionApplication.class, args);
    }
}
