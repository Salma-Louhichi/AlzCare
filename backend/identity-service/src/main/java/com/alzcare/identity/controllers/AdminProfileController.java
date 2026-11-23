package com.alzcare.identity.controllers;

import com.alzcare.identity.dtos.requests.CaregiverUpdateRequest;
import com.alzcare.identity.dtos.requests.DoctorUpdateRequest;
import com.alzcare.identity.dtos.requests.PatientUpdateRequest;
import com.alzcare.identity.dtos.responses.CaregiverProfileResponse;
import com.alzcare.identity.dtos.responses.DoctorProfileResponse;
import com.alzcare.identity.dtos.responses.PatientProfileResponse;
import com.alzcare.identity.dtos.responses.UserResponse;
import com.alzcare.identity.services.CaregiverProfileService;
import com.alzcare.identity.services.DoctorProfileService;
import com.alzcare.identity.services.KeycloakAdminService;
import com.alzcare.identity.services.PatientProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for admin profile management operations.
 * Provides endpoints for administrators to view and edit any user's profile
 * regardless of their role (Patient, Doctor, or Caregiver).
 */
@RestController
@RequestMapping("/api/v1/admin/profiles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Profile Management", description = "Admin APIs for managing user profiles across all roles")
public class AdminProfileController {

    private final KeycloakAdminService keycloakAdminService;
    private final PatientProfileService patientProfileService;
    private final DoctorProfileService doctorProfileService;
    private final CaregiverProfileService caregiverProfileService;

    /**
     * Get a user's profile by their Keycloak user ID.
     * The role is determined from Keycloak and the appropriate profile is returned.
     *
     * @param userId the Keycloak user ID
     * @return the user's profile (PatientProfileResponse, DoctorProfileResponse, or CaregiverProfileResponse)
     */
    @GetMapping("/{userId}")
    @Operation(
            summary = "Get user profile by ID",
            description = "Retrieves a user's profile by their Keycloak user ID. Returns the appropriate profile type based on the user's role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile found",
                    content = @Content(schema = @Schema(oneOf = {
                            PatientProfileResponse.class,
                            DoctorProfileResponse.class,
                            CaregiverProfileResponse.class
                    }))),
            @ApiResponse(responseCode = "404", description = "User or profile not found"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID format")
    })
    public ResponseEntity<?> getUserProfile(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathVariable String userId) {
        
        log.debug("Admin requesting profile for user: {}", userId);
        
        // Get user from Keycloak to determine role
        UserResponse user = keycloakAdminService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        UUID userUuid = UUID.fromString(userId);
        
        // Return appropriate profile based on role
        return switch (user.getRole()) {
            case PATIENT -> {
                PatientProfileResponse profile = patientProfileService.getPatientByUserId(userUuid);
                yield ResponseEntity.ok(profile);
            }
            case DOCTOR -> {
                DoctorProfileResponse profile = doctorProfileService.getDoctorByUserId(userUuid);
                yield ResponseEntity.ok(profile);
            }
            case CAREGIVER -> {
                CaregiverProfileResponse profile = caregiverProfileService.getCaregiverByUserId(userUuid);
                yield ResponseEntity.ok(profile);
            }
            case ADMIN -> {
                // Admins typically don't have profiles in the same way
                log.warn("Attempted to get profile for admin user: {}", userId);
                yield ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Admin users do not have profiles"));
            }
        };
    }

    /**
     * Check if a profile exists for a user.
     *
     * @param userId the Keycloak user ID
     * @return map containing exists status
     */
    @GetMapping("/{userId}/exists")
    @Operation(
            summary = "Check if profile exists",
            description = "Checks whether a profile exists for the specified user ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check completed successfully"),
            @ApiResponse(responseCode = "404", description = "User not found in Keycloak")
    })
    public ResponseEntity<Map<String, Object>> checkProfileExists(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathVariable String userId) {
        
        log.debug("Admin checking profile existence for user: {}", userId);
        
        // Verify user exists in Keycloak
        UserResponse user = keycloakAdminService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        UUID userUuid = UUID.fromString(userId);
        boolean exists = false;
        String profileType = null;
        
        try {
            switch (user.getRole()) {
                case PATIENT -> {
                    patientProfileService.getPatientByUserId(userUuid);
                    exists = true;
                    profileType = "PATIENT";
                }
                case DOCTOR -> {
                    doctorProfileService.getDoctorByUserId(userUuid);
                    exists = true;
                    profileType = "DOCTOR";
                }
                case CAREGIVER -> {
                    caregiverProfileService.getCaregiverByUserId(userUuid);
                    exists = true;
                    profileType = "CAREGIVER";
                }
                case ADMIN -> {
                    exists = false;
                    profileType = "ADMIN";
                }
            }
        } catch (Exception e) {
            log.debug("Profile not found for user {}: {}", userId, e.getMessage());
            exists = false;
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("userId", userId);
        response.put("role", user.getRole());
        response.put("profileType", profileType);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update a user's profile by their Keycloak user ID.
     * Accepts role-specific update requests.
     *
     * @param userId  the Keycloak user ID
     * @param request the update request (should match the user's role)
     * @return the updated profile
     */
    @PutMapping("/{userId}")
    @Operation(
            summary = "Update user profile",
            description = "Updates a user's profile by their Keycloak user ID. The request body must match the user's role type."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(oneOf = {
                            PatientProfileResponse.class,
                            DoctorProfileResponse.class,
                            CaregiverProfileResponse.class
                    }))),
            @ApiResponse(responseCode = "404", description = "User or profile not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or role mismatch"),
            @ApiResponse(responseCode = "409", description = "Conflict with existing data")
    })
    public ResponseEntity<?> updateUserProfile(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathVariable String userId,
            @Parameter(description = "Profile update request (role-specific)", required = true)
            @Valid @RequestBody Object request) {
        
        log.debug("Admin updating profile for user: {}", userId);
        
        // Get user from Keycloak to determine role
        UserResponse user = keycloakAdminService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        UUID userUuid = UUID.fromString(userId);
        
        // Validate and process based on role
        return switch (user.getRole()) {
            case PATIENT -> {
                if (!(request instanceof PatientUpdateRequest)) {
                    throw new IllegalArgumentException(
                            "Invalid request type for PATIENT role. Expected PatientUpdateRequest");
                }
                PatientProfileResponse updated = patientProfileService.updatePatientByUserId(userUuid, 
                        (PatientUpdateRequest) request);
                yield ResponseEntity.ok(updated);
            }
            case DOCTOR -> {
                if (!(request instanceof DoctorUpdateRequest)) {
                    throw new IllegalArgumentException(
                            "Invalid request type for DOCTOR role. Expected DoctorUpdateRequest");
                }
                DoctorProfileResponse updated = doctorProfileService.updateDoctorByUserId(userUuid, 
                        (DoctorUpdateRequest) request);
                yield ResponseEntity.ok(updated);
            }
            case CAREGIVER -> {
                if (!(request instanceof CaregiverUpdateRequest)) {
                    throw new IllegalArgumentException(
                            "Invalid request type for CAREGIVER role. Expected CaregiverUpdateRequest");
                }
                CaregiverProfileResponse updated = caregiverProfileService.updateCaregiverByUserId(userUuid, 
                        (CaregiverUpdateRequest) request);
                yield ResponseEntity.ok(updated);
            }
            case ADMIN -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Admin users do not have profiles to update"));
        };
    }

    /**
     * Update a patient profile specifically.
     * Convenience endpoint for updating patient profiles.
     *
     * @param userId  the Keycloak user ID
     * @param request the patient update request
     * @return the updated patient profile
     */
    @PutMapping("/{userId}/patient")
    @Operation(
            summary = "Update patient profile",
            description = "Updates a patient profile by their Keycloak user ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient profile updated successfully",
                    content = @Content(schema = @Schema(implementation = PatientProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "Patient profile not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or user is not a patient")
    })
    public ResponseEntity<PatientProfileResponse> updatePatientProfile(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathVariable String userId,
            @Valid @RequestBody PatientUpdateRequest request) {
        
        log.debug("Admin updating patient profile for user: {}", userId);
        
        // Verify user is a patient
        UserResponse user = keycloakAdminService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        if (user.getRole() != UserResponse.UserRole.PATIENT) {
            throw new IllegalArgumentException("User is not a patient. Role: " + user.getRole());
        }
        
        PatientProfileResponse updated = patientProfileService.updatePatientByUserId(
                UUID.fromString(userId), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Update a doctor profile specifically.
     * Convenience endpoint for updating doctor profiles.
     *
     * @param userId  the Keycloak user ID
     * @param request the doctor update request
     * @return the updated doctor profile
     */
    @PutMapping("/{userId}/doctor")
    @Operation(
            summary = "Update doctor profile",
            description = "Updates a doctor profile by their Keycloak user ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Doctor profile updated successfully",
                    content = @Content(schema = @Schema(implementation = DoctorProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "Doctor profile not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or user is not a doctor")
    })
    public ResponseEntity<DoctorProfileResponse> updateDoctorProfile(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathVariable String userId,
            @Valid @RequestBody DoctorUpdateRequest request) {
        
        log.debug("Admin updating doctor profile for user: {}", userId);
        
        // Verify user is a doctor
        UserResponse user = keycloakAdminService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        if (user.getRole() != UserResponse.UserRole.DOCTOR) {
            throw new IllegalArgumentException("User is not a doctor. Role: " + user.getRole());
        }
        
        DoctorProfileResponse updated = doctorProfileService.updateDoctorByUserId(
                UUID.fromString(userId), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Update a caregiver profile specifically.
     * Convenience endpoint for updating caregiver profiles.
     *
     * @param userId  the Keycloak user ID
     * @param request the caregiver update request
     * @return the updated caregiver profile
     */
    @PutMapping("/{userId}/caregiver")
    @Operation(
            summary = "Update caregiver profile",
            description = "Updates a caregiver profile by their Keycloak user ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Caregiver profile updated successfully",
                    content = @Content(schema = @Schema(implementation = CaregiverProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "Caregiver profile not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or user is not a caregiver")
    })
    public ResponseEntity<CaregiverProfileResponse> updateCaregiverProfile(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathVariable String userId,
            @Valid @RequestBody CaregiverUpdateRequest request) {
        
        log.debug("Admin updating caregiver profile for user: {}", userId);
        
        // Verify user is a caregiver
        UserResponse user = keycloakAdminService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        if (user.getRole() != UserResponse.UserRole.CAREGIVER) {
            throw new IllegalArgumentException("User is not a caregiver. Role: " + user.getRole());
        }
        
        CaregiverProfileResponse updated = caregiverProfileService.updateCaregiverByUserId(
                UUID.fromString(userId), request);
        return ResponseEntity.ok(updated);
    }
}
