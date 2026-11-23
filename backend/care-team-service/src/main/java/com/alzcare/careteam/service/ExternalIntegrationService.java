package com.alzcare.careteam.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Module 3.1 / 3.2 external APIs from PDF (TomTom stays in TomTomService).
 */
@Service
@Slf4j
public class ExternalIntegrationService {

    private final RestTemplate restTemplate;

    @Value("${alzcare.integrations.airvisual.api-key:}")
    private String airVisualKey;

    @Value("${alzcare.integrations.newsapi.api-key:}")
    private String newsApiKey;

    public ExternalIntegrationService(RestTemplate careTeamRestTemplate) {
        this.restTemplate = careTeamRestTemplate;
    }

    /** RxNorm — no API key (NLM). */
    public String rxNormSearchByName(String name) {
        String url = UriComponentsBuilder
                .fromUriString("https://rxnav.nlm.nih.gov/REST/rxcui.json")
                .queryParam("name", name)
                .build()
                .toUriString();
        ResponseEntity<String> r = restTemplate.getForEntity(url, String.class);
        return r.getBody();
    }

    /** AirVisual — optional key; returns error JSON if missing. */
    public String airQualityNearest(double lat, double lon) {
        if (airVisualKey == null || airVisualKey.isBlank()) {
            return "{\"error\":\"AIRVISUAL_API_KEY not configured\"}";
        }
        String url = UriComponentsBuilder
                .fromUriString("https://api.airvisual.com/v2/nearest_city")
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("key", airVisualKey)
                .build()
                .toUriString();
        try {
            return restTemplate.getForEntity(url, String.class).getBody();
        } catch (Exception e) {
            log.warn("AirVisual request failed: {}", e.getMessage());
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /** NewsAPI — optional key. */
    public String alzheimerResearchNews() {
        if (newsApiKey == null || newsApiKey.isBlank()) {
            return "{\"status\":\"error\",\"message\":\"NEWS_API_KEY not configured\"}";
        }
        String url = UriComponentsBuilder
                .fromUriString("https://newsapi.org/v2/everything")
                .queryParam("q", "Alzheimer")
                .queryParam("language", "en")
                .queryParam("sortBy", "publishedAt")
                .queryParam("pageSize", "10")
                .queryParam("apiKey", newsApiKey)
                .build()
                .toUriString();
        try {
            return restTemplate.getForEntity(url, String.class).getBody();
        } catch (Exception e) {
            log.warn("NewsAPI request failed: {}", e.getMessage());
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
}
