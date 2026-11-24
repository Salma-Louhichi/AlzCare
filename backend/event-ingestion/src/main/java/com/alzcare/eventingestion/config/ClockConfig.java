package com.alzcare.eventingestion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Injected everywhere "now" matters, so tests can control time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
