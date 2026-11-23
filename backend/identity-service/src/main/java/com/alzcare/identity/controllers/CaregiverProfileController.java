package com.alzcare.identity.controllers;

import com.alzcare.identity.dtos.responses.CaregiverProfileResponse;
import com.alzcare.identity.services.CaregiverProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for managing caregiver profiles.
 * Provides endpoints for retrieving caregiver profile information.
 */
@RestController
@RequestMapping("/api/v1/caregivers")
@Tag(name = "Caregiver Profile", description = "Caregiver profile management APIs")
public class CaregiverProfileController {

    private final CaregiverProfileService caregiverProfileService;

    /**
     * Constructs a new CaregiverProfileController with the required service.
     *
     * @param caregiverProfileService the service for caregiver profile operations
     */
    public CaregiverProfileController(CaregiverProfileService caregiverProfileService) {
        this.caregiverProfileService = caregiverProfileService;
    }

    /**
     * Retrieves a caregiver profile by their internal ID.
     *
     * @param id the caregiver profile UUID
     * @return the caregiver profile with HTTP 200 status
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get caregiver by ID", description = "Retrieves a caregiver profile using their internal UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Caregiver profile found",
                    content = @Content(schema = @Schema(implementation = CaregiverProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "Caregiver profile not found")
    })
    public ResponseEntity<CaregiverProfileResponse> getCaregiverById(
            @Parameter(description = "Caregiver profile UUID", required = true)
            @PathVariable UUID id) {
        CaregiverProfileResponse response = caregiverProfileService.getCaregiverById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a caregiver profile by their user ID (typically from Keycloak).
     *
     * @param userId the user ID
     * @return the caregiver profile with HTTP 200 status
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get caregiver by user ID", description = "Retrieves a caregiver profile using their user ID (typically from Keycloak)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Caregiver profile found",
                    content = @Content(schema = @Schema(implementation = CaregiverProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "Caregiver profile not found")
    })
    public ResponseEntity<CaregiverProfileResponse> getCaregiverByUserId(
            @Parameter(description = "User ID (typically Keycloak user ID)", required = true)
            @PathVariable UUID userId) {
        CaregiverProfileResponse response = caregiverProfileService.getCaregiverByUserId(userId);
        return ResponseEntity.ok(response);
    }
}
