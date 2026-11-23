package com.alzcare.identity.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result of doctor-driven patient provisioning (identity + care-team assignment).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorCreatesPatientResponse {

    private String patientUserId;
    private String doctorId;
    private String assignmentId;
    private String assignmentStatus;
    private String generatedPassword;
}
