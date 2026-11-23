package com.alzcare.identity.controllers;

import com.alzcare.identity.dtos.requests.AutonomyAssessmentRequest;
import com.alzcare.identity.dtos.responses.AutonomyAssessmentResponse;
import com.alzcare.identity.services.AutonomyAssessmentService;
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
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for managing patient autonomy assessments.
 * Provides endpoints for creating and retrieving autonomy assessments.
 */
@RestController
@RequestMapping("/api/v1/patients/{patientId}/autonomy")
@Tag(name = "Autonomy Assessment", description = "Patient autonomy assessment management APIs")
public class AutonomyAssessmentController {

    private final AutonomyAssessmentService autonomyAssessmentService;

    /**
     * Constructs a new AutonomyAssessmentController with the required service.
     *
     * @param autonomyAssessmentService the service for autonomy assessment operations
     */
    public AutonomyAssessmentController(AutonomyAssessmentService autonomyAssessmentService) {
        this.autonomyAssessmentService = autonomyAssessmentService;
    }

    /**
     * Creates a new autonomy assessment for a patient.
     *
     * @param patientId the patient profile UUID
     * @param request   the assessment request containing domain levels
     * @return the created autonomy assessment with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create autonomy assessment", description = "Creates a new autonomy assessment for a patient. Overall score is automatically calculated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Autonomy assessment created successfully",
                    content = @Content(schema = @Schema(implementation = AutonomyAssessmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Patient profile not found")
    })
    public ResponseEntity<AutonomyAssessmentResponse> createAssessment(
            @Parameter(description = "Patient profile UUID", required = true)
            @PathVariable UUID patientId,
            @Valid @RequestBody AutonomyAssessmentRequest request) {
        AutonomyAssessmentResponse response = autonomyAssessmentService.createAssessment(patientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves the most recent autonomy assessment for a patient.
     *
     * @param patientId the patient profile UUID
     * @return the latest autonomy assessment with HTTP 200 status, or HTTP 404 if none exists
     */
    @GetMapping
    @Operation(summary = "Get latest autonomy assessment", description = "Retrieves the most recent autonomy assessment for a patient")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest assessment found",
                    content = @Content(schema = @Schema(implementation = AutonomyAssessmentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Patient profile not found or no assessments exist")
    })
    public ResponseEntity<AutonomyAssessmentResponse> getLatestAssessment(
            @Parameter(description = "Patient profile UUID", required = true)
            @PathVariable UUID patientId) {
        Optional<AutonomyAssessmentResponse> response = autonomyAssessmentService.getLatestAssessment(patientId);
        return response.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves the complete history of autonomy assessments for a patient.
     *
     * @param patientId the patient profile UUID
     * @return a list of autonomy assessments ordered from most recent to oldest
     */
    @GetMapping("/history")
    @Operation(summary = "Get assessment history", description = "Retrieves the complete history of autonomy assessments for a patient, ordered from most recent to oldest")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessment history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AutonomyAssessmentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Patient profile not found")
    })
    public ResponseEntity<List<AutonomyAssessmentResponse>> getAssessmentHistory(
            @Parameter(description = "Patient profile UUID", required = true)
            @PathVariable UUID patientId) {
        List<AutonomyAssessmentResponse> response = autonomyAssessmentService.getAssessmentHistory(patientId);
        return ResponseEntity.ok(response);
    }
}
