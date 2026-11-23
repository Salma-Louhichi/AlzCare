package com.alzcare.careteam.controller;

import com.alzcare.careteam.service.ExternalIntegrationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/care-team/integrations")
public class IntegrationsController {

    private final ExternalIntegrationService integrations;

    public IntegrationsController(ExternalIntegrationService integrations) {
        this.integrations = integrations;
    }

    @GetMapping(value = "/rxnorm/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> rxNorm(@RequestParam String name) {
        return ResponseEntity.ok(integrations.rxNormSearchByName(name));
    }

    @GetMapping(value = "/air-quality", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> airQuality(
            @RequestParam double lat,
            @RequestParam double lon) {
        return ResponseEntity.ok(integrations.airQualityNearest(lat, lon));
    }

    @GetMapping(value = "/research-news", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> researchNews() {
        return ResponseEntity.ok(integrations.alzheimerResearchNews());
    }
}
