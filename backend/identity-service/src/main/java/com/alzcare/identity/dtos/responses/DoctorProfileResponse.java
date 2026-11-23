package com.alzcare.identity.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for doctor profile data.
 * Includes professional details and contact information.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfileResponse {

    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String photoUrl;
    private String speciality;
    private String licenseNumber;
    private String phone;
    private String contact;
    private String address;
    private Boolean isAvailable;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
