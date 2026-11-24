package com.alzcare.eventingestion.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** A risky behaviour inferred from a series of motion events. */
@Entity
@Table(name = "detected_patterns")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectedPattern {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PatternType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PatternSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PatternStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at", nullable = false)
    private Instant endedAt;

    @Column(name = "event_count", nullable = false)
    private int eventCount;

    /** Comma-separated zone names, in the order first seen. */
    @Column(nullable = false)
    private String zones;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(nullable = false)
    private boolean notified;

    @Column(name = "acknowledged_by")
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;
}
