package com.alzcare.identity.dtos.requests;

import com.alzcare.identity.enums.AutonomyLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a new autonomy assessment.
 * Contains autonomy levels across different domains for a patient.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutonomyAssessmentRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "Hygiene level is required")
    private AutonomyLevel hygieneLevel;

    @NotNull(message = "Medication level is required")
    private AutonomyLevel medicationLevel;

    @NotNull(message = "Mobility level is required")
    private AutonomyLevel mobilityLevel;

    @NotNull(message = "Feeding level is required")
    private AutonomyLevel feedingLevel;

    @NotNull(message = "Assessed by is required")
    private UUID assessedBy;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    @NotNull(message = "Assessment date is required")
    private LocalDate assessmentDate;
}
