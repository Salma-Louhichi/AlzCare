package com.alzcare.identity.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PDF module 3.2 — doctor creates patient account (Keycloak + profile) and is auto-assigned in care-team.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorCreatePatientRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    private String email;

    /**
     * If blank, a temporary password is generated (doctor should reset via admin or share once).
     */
    @Size(min = 8, max = 128)
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    /** ISO date yyyy-MM-dd */
    @NotBlank
    private String dateOfBirth;

    /** MALE or FEMALE (identity patient profile) */
    @NotBlank
    private String gender;

    private String phone;
    private String address;
    private String emergencyContact;

    /** Stored on care-team patient care profile (diagnosis / stage) */
    private String diagnosisStage;

    @Builder.Default
    private Boolean emailVerified = true;
}
