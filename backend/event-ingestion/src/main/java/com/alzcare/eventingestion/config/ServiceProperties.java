package com.alzcare.eventingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alzcare.services")
public record ServiceProperties(String careTeamUrl, String identityUrl) {
}
