package com.alzcare.eventingestion.repository;

import com.alzcare.eventingestion.domain.DetectedPattern;
import com.alzcare.eventingestion.domain.PatternType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DetectedPatternRepository extends JpaRepository<DetectedPattern, UUID> {

    /** Patterns of a type whose time span reaches {@code from} or later - candidates to extend. */
    List<DetectedPattern> findByPatientIdAndTypeAndEndedAtGreaterThanEqual(
            String patientId, PatternType type, Instant from);

    List<DetectedPattern> findByPatientIdAndDetectedAtAfterOrderByDetectedAtDesc(String patientId, Instant after);

    List<DetectedPattern> findByNotifiedFalseAndDetectedAtAfter(Instant after);
}
