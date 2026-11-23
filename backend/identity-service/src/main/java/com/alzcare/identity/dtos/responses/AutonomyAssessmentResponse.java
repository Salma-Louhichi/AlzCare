package com.alzcare.identity.dtos.responses;

import com.alzcare.identity.enums.AutonomyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for autonomy assessment data.
 * Includes autonomy levels across different domains and calculated overall score.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutonomyAssessmentResponse {

    private UUID id;
    private UUID patientId;
    private AutonomyLevel hygieneLevel;
    private AutonomyLevel medicationLevel;
    private AutonomyLevel mobilityLevel;
    private AutonomyLevel feedingLevel;
    private Integer overallScore;
    private UUID assessedBy;
    private String notes;
    private LocalDate assessmentDate;
    private OffsetDateTime createdAt;
}
