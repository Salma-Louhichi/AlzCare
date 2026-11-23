package com.alzcare.identity.controllers;

import com.alzcare.identity.dtos.requests.PatientCreateRequest;
import com.alzcare.identity.dtos.requests.PatientUpdateRequest;
import com.alzcare.identity.dtos.responses.PatientProfileResponse;
import com.alzcare.identity.services.PatientProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing patient profiles.
 * Provides endpoints for creating, retrieving, and updating patient profiles.
 */
@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patient Profile", description = "Patient profile management APIs")
public class PatientProfileController {

    private final PatientProfileService patientProfileService;

    /**
     * Constructs a new PatientProfileController with the required service.
     *
     * @param patientProfileService the service for patient profile operations
     */
    public PatientProfileController(PatientProfileService patientProfileService) {
        this.patientProfileService = patientProfileService;
    }

    /**
     * Creates a new patient profile.
     *
     * @param request the patient creation request
     * @return the created patient profile with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create patient profile", description = "Creates a new patient profile linked to Keycloak")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Patient profile created successfully",
                    content = @Content(schema = @Schema(implementation = PatientProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or patient already exists"),
            @ApiResponse(responseCode = "409", description = "Patient with same Keycloak ID already exists")
    })
    public ResponseEntity<PatientProfileResponse> createPatient(
            @Valid @RequestBody PatientCreateRequest request) {
        PatientProfileResponse response = patientProfileService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a patient profile by their User ID (Keycloak ID).
     *
     * @param userId the Keycloak user ID
     * @return the patient profile with HTTP 200 status
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get patient by User ID", description = "Retrieves a patient profile using their Keycloak user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient profile found",
                    content = @Content(schema = @Schema(implementation = PatientProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "Patient profile not found")
    })
    public ResponseEntity<PatientProfileResponse> getPatientByUserId(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathVariable UUID userId) {
        PatientProfileResponse response = patientProfileService.getPatientByUserId(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing patient profile.
     *
     * @param id      the patient profile UUID
     * @param request the update request containing fields to modify
     * @return the updated patient profile with HTTP 200 status
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update patient profile", description = "Updates an existing patient profile. Only provided fields are modified.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient profile updated successfully",
                    content = @Content(schema = @Schema(implementation = PatientProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Patient profile not found")
    })
    public ResponseEntity<PatientProfileResponse> updatePatient(
            @Parameter(description = "Patient profile UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody PatientUpdateRequest request) {
        PatientProfileResponse response = patientProfileService.updatePatient(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all patient profiles.
     *
     * @param isActive optional filter for active status
     * @return a list of patient profiles with HTTP 200 status
     */
    @GetMapping
    @Operation(summary = "Get all patients", description = "Retrieves all patient profiles, optionally filtered by active status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient profiles retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PatientProfileResponse.class)))
    })
    public ResponseEntity<List<PatientProfileResponse>> getAllPatients(
            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean isActive) {
        List<PatientProfileResponse> responses = patientProfileService.getAllPatients(isActive);
        return ResponseEntity.ok(responses);
    }
}
