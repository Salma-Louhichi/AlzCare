package com.alzcare.eventingestion.web;

import com.alzcare.eventingestion.service.PatientAccess;
import com.alzcare.eventingestion.service.PatternService;
import com.alzcare.eventingestion.web.Dtos.PatternResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patterns")
public class PatternController {

    private final PatternService patterns;
    private final PatientAccess access;

    public PatternController(PatternService patterns, PatientAccess access) {
        this.patterns = patterns;
        this.access = access;
    }

    @GetMapping("/patient/{patientId}")
    public List<PatternResponse> forPatient(@PathVariable String patientId,
                                            @RequestParam(defaultValue = "30") int days,
                                            @AuthenticationPrincipal Jwt jwt) {
        access.check(jwt, patientId);
        int bounded = Math.max(1, Math.min(days, 365));
        return patterns.recentForPatient(patientId, Duration.ofDays(bounded)).stream().map(PatternResponse::of).toList();
    }

    @PutMapping("/{id}/acknowledge")
    public PatternResponse acknowledge(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        access.check(jwt, patterns.get(id).getPatientId());
        return PatternResponse.of(patterns.acknowledge(id, jwt.getSubject()));
    }
}
