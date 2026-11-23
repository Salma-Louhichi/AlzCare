package com.alzcare.identity.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for caregiver profile data.
 * Includes caregiver details and availability status.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaregiverProfileResponse {

    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String photoUrl;
    private String phone;
    private String address;
    private String contact;
    private Boolean isAvailable;
    private Boolean isProfessional;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
