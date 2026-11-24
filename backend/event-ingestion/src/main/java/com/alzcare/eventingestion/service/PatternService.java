package com.alzcare.eventingestion.service;

import com.alzcare.eventingestion.domain.DetectedPattern;
import com.alzcare.eventingestion.domain.PatternStatus;
import com.alzcare.eventingestion.repository.DetectedPatternRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class PatternService {

    private final DetectedPatternRepository patterns;
    private final Clock clock;

    public PatternService(DetectedPatternRepository patterns, Clock clock) {
        this.patterns = patterns;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<DetectedPattern> recentForPatient(String patientId, Duration window) {
        return patterns.findByPatientIdAndDetectedAtAfterOrderByDetectedAtDesc(patientId, clock.instant().minus(window));
    }

    @Transactional(readOnly = true)
    public DetectedPattern get(UUID id) {
        return patterns.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pattern not found"));
    }

    @Transactional
    public DetectedPattern acknowledge(UUID id, String userId) {
        DetectedPattern pattern = get(id);
        if (pattern.getStatus() == PatternStatus.OPEN) {
            pattern.setStatus(PatternStatus.ACKNOWLEDGED);
            pattern.setAcknowledgedBy(userId);
            pattern.setAcknowledgedAt(clock.instant());
        }
        return pattern;
    }
}
