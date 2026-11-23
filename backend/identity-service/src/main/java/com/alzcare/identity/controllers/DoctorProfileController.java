package com.alzcare.identity.controllers;

import com.alzcare.identity.dtos.responses.DoctorProfileResponse;
import com.alzcare.identity.services.DoctorProfileService;
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
 * REST controller for managing doctor profiles.
 * Provides endpoints for retrieving doctor profile information.
 */
@RestController
@RequestMapping("/api/v1/doctors")
@Tag(name = "Doctor Profile", description = "Doctor profile management APIs")
public class DoctorProfileController {

    private final DoctorProfileService doctorProfileService;

    /**
     * Constructs a new DoctorProfileController with the required service.
     *
     * @param doctorProfileService the service for doctor profile operations
     */
    public DoctorProfileController(DoctorProfileService doctorProfileService) {
        this.doctorProfileService = doctorProfileService;
    }

    /**
     * Retrieves a doctor profile by their internal ID.
     *
     * @param id the doctor profile UUID
     * @return the doctor profile with HTTP 200 status
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get doctor by ID", description = "Retrieves a doctor profile using their internal UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Doctor profile found",
                    content = @Content(schema = @Schema(implementation = DoctorProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "Doctor profile not found")
    })
    public ResponseEntity<DoctorProfileResponse> getDoctorById(
            @Parameter(description = "Doctor profile UUID", required = true)
            @PathVariable UUID id) {
        DoctorProfileResponse response = doctorProfileService.getDoctorById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a doctor profile by their user ID (typically from Keycloak).
     *
     * @param userId the user ID
     * @return the doctor profile with HTTP 200 status
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get doctor by user ID", description = "Retrieves a doctor profile using their user ID (typically from Keycloak)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Doctor profile found",
                    content = @Content(schema = @Schema(implementation = DoctorProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "Doctor profile not found")
    })
    public ResponseEntity<DoctorProfileResponse> getDoctorByUserId(
            @Parameter(description = "User ID (typically Keycloak user ID)", required = true)
            @PathVariable UUID userId) {
        DoctorProfileResponse response = doctorProfileService.getDoctorByUserId(userId);
        return ResponseEntity.ok(response);
    }
}
