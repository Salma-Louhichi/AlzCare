package com.alzcare.identity.dtos.requests;

import com.alzcare.identity.enums.GenderEnum;
import com.alzcare.identity.enums.LanguageEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request DTO for updating an existing patient profile.
 * All fields are optional - only provided fields will be updated.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientUpdateRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private GenderEnum gender;

    private LanguageEnum preferredLanguage;

    @Size(max = 500, message = "Photo URL must not exceed 500 characters")
    private String photoUrl;

    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;

    @Size(max = 255, message = "Emergency contact must not exceed 255 characters")
    private String emergencyContact;

    @Size(max = 1000, message = "Address must not exceed 1000 characters")
    private String address;

    @NotNull(message = "Active status is required")
    private Boolean isActive;

    private Integer totalPoints;

    private Integer currentStreak;

    private LocalDate lastPlayedDate;

    private Boolean assistedModeActive;
}
