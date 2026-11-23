package com.alzcare.careteam.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TomTomService {

    @Value("${tomtom.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Object getRoute(
            double originLat, double originLon,
            double destLat, double destLon) {

        String url = String.format(
                "https://api.tomtom.com/routing/1/calculateRoute/" +
                        "%f,%f:%f,%f/json?key=%s&traffic=true",
                originLat, originLon, destLat, destLon, apiKey
        );
        return restTemplate.getForObject(url, Object.class);
    }
}