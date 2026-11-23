package com.alzcare.identity.dtos.responses;

import com.alzcare.identity.enums.GenderEnum;
import com.alzcare.identity.enums.LanguageEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for patient profile data.
 * Includes patient demographics, preferences, and timestamps.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfileResponse {

    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private GenderEnum gender;
    private LanguageEnum preferredLanguage;
    private String photoUrl;
    private String phone;
    private String emergencyContact;
    private String address;
    private Boolean isActive;
    private Long version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private Integer totalPoints;
    private Integer currentStreak;
    private LocalDate lastPlayedDate;
    private Boolean assistedModeActive;
}
