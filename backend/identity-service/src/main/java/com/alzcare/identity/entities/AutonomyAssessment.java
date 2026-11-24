package com.alzcare.identity.entities;

import com.alzcare.identity.enums.AutonomyLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Patient autonomy level assessments across different domains.
 * Tracks independence levels for hygiene, medication, mobility, and feeding.
 */
@Entity
@Table(name = "autonomy_assessments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutonomyAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientProfile patient;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // PostgreSQL enum type autonomy_level
    @Column(name = "hygiene_level", nullable = false, columnDefinition = "autonomy_level")
    private AutonomyLevel hygieneLevel;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // PostgreSQL enum type autonomy_level
    @Column(name = "medication_level", nullable = false, columnDefinition = "autonomy_level")
    private AutonomyLevel medicationLevel;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // PostgreSQL enum type autonomy_level
    @Column(name = "mobility_level", nullable = false, columnDefinition = "autonomy_level")
    private AutonomyLevel mobilityLevel;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // PostgreSQL enum type autonomy_level
    @Column(name = "feeding_level", nullable = false, columnDefinition = "autonomy_level")
    private AutonomyLevel feedingLevel;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "assessed_by", nullable = false)
    private UUID assessedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Calculate overall score based on domain levels.
     * Maps the sum (4-12) to a 1-10 scale.
     * This is a convenience method that can be called before saving.
     */
    @PrePersist
    @PreUpdate
    public void calculateOverallScore() {
        if (hygieneLevel != null && medicationLevel != null && 
            mobilityLevel != null && feedingLevel != null) {
            int sum = 0;
            sum += getLevelValue(hygieneLevel);
            sum += getLevelValue(medicationLevel);
            sum += getLevelValue(mobilityLevel);
            sum += getLevelValue(feedingLevel);
            // Map 4-12 to the 1-10 scale enforced by the overall_score CHECK constraint.
            // (The previous formula, (sum - 4) * 10 / 8 + 1, produced 11 for an all-INDEPENDENT
            // assessment, which the database rejected.)
            this.overallScore = 1 + (sum - 4) * 9 / 8;
        }
    }

    private int getLevelValue(AutonomyLevel level) {
        return switch (level) {
            case INDEPENDENT -> 3;
            case ASSISTED -> 2;
            case DEPENDENT -> 1;
        };
    }
}
