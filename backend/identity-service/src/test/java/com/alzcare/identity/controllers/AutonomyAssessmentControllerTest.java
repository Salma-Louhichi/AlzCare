package com.alzcare.identity.controllers;

import com.alzcare.identity.dtos.requests.AutonomyAssessmentRequest;
import com.alzcare.identity.dtos.responses.AutonomyAssessmentResponse;
import com.alzcare.identity.enums.AutonomyLevel;
import com.alzcare.identity.exceptions.PatientNotFoundException;
import com.alzcare.identity.services.AutonomyAssessmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link AutonomyAssessmentController}.
 * Tests REST endpoints for autonomy assessment management using mocked service layer.
 */
@WebMvcTest(AutonomyAssessmentController.class)
@ContextConfiguration(classes = {AutonomyAssessmentController.class, GlobalExceptionHandler.class})
@DisplayName("Autonomy Assessment Controller Tests")
class AutonomyAssessmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AutonomyAssessmentService autonomyAssessmentService;

    private UUID patientId;
    private UUID assessmentId;
    private AutonomyAssessmentRequest validRequest;
    private AutonomyAssessmentResponse assessmentResponse;
    private OffsetDateTime assessedAt;
    private OffsetDateTime createdAt;

    @BeforeEach
    void setUp() {
        patientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assessmentId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
        assessedAt = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);
        createdAt = OffsetDateTime.of(2024, 1, 15, 10, 35, 0, 0, ZoneOffset.UTC);

        validRequest = createValidRequest();
        assessmentResponse = createAssessmentResponse();
    }

    private AutonomyAssessmentRequest createValidRequest() {
        AutonomyAssessmentRequest request = new AutonomyAssessmentRequest();
        request.setPatientId(patientId);
        request.setHygieneLevel(AutonomyLevel.INDEPENDENT);
        request.setMedicationLevel(AutonomyLevel.ASSISTED);
        request.setMobilityLevel(AutonomyLevel.ASSISTED);
        request.setFeedingLevel(AutonomyLevel.INDEPENDENT);
        request.setAssessedBy("Dr. Sarah Johnson");
        request.setNotes("Patient shows good progress in mobility");
        request.setAssessedAt(assessedAt);
        return request;
    }

    private AutonomyAssessmentResponse createAssessmentResponse() {
        AutonomyAssessmentResponse response = new AutonomyAssessmentResponse();
        response.setId(assessmentId);
        response.setPatientId(patientId);
        response.setHygieneLevel(AutonomyLevel.INDEPENDENT);
        response.setMedicationLevel(AutonomyLevel.ASSISTED);
        response.setMobilityLevel(AutonomyLevel.ASSISTED);
        response.setFeedingLevel(AutonomyLevel.INDEPENDENT);
        response.setOverallScore(75);
        response.setAssessedBy("Dr. Sarah Johnson");
        response.setNotes("Patient shows good progress in mobility");
        response.setAssessedAt(assessedAt);
        response.setCreatedAt(createdAt);
        return response;
    }

    // ====================================================================================
    // POST /api/v1/patients/{patientId}/autonomy
    // ====================================================================================

    @Test
    @DisplayName("Should create assessment and return 201 when request is valid")
    void shouldCreateAssessmentAndReturn201WhenRequestIsValid() throws Exception {
        // Given
        when(autonomyAssessmentService.createAssessment(eq(patientId), any(AutonomyAssessmentRequest.class)))
                .thenReturn(assessmentResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/patients/{patientId}/autonomy", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(assessmentId.toString()))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.hygieneLevel").value("INDEPENDENT"))
                .andExpect(jsonPath("$.medicationLevel").value("ASSISTED"))
                .andExpect(jsonPath("$.mobilityLevel").value("ASSISTED"))
                .andExpect(jsonPath("$.feedingLevel").value("INDEPENDENT"))
                .andExpect(jsonPath("$.overallScore").value(75))
                .andExpect(jsonPath("$.assessedBy").value("Dr. Sarah Johnson"))
                .andExpect(jsonPath("$.notes").value("Patient shows good progress in mobility"))
                .andExpect(jsonPath("$.assessedAt").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        verify(autonomyAssessmentService).createAssessment(eq(patientId), any(AutonomyAssessmentRequest.class));
    }

    @Test
    @DisplayName("Should return 404 when patient not found during assessment creation")
    void shouldReturn404WhenPatientNotFoundDuringAssessmentCreation() throws Exception {
        // Given
        when(autonomyAssessmentService.createAssessment(eq(patientId), any(AutonomyAssessmentRequest.class)))
                .thenThrow(new PatientNotFoundException(patientId));

        // When & Then
        mockMvc.perform(post("/api/v1/patients/{patientId}/autonomy", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Patient Not Found"))
                .andExpect(jsonPath("$.detail").value("Patient profile not found with id: " + patientId))
                .andExpect(jsonPath("$.type").value("https://alzcare.com/errors/patient-not-found"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(autonomyAssessmentService).createAssessment(eq(patientId), any(AutonomyAssessmentRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when request body is missing required fields")
    void shouldReturn400WhenRequestBodyIsMissingRequiredFields() throws Exception {
        // Given - request with missing required fields
        AutonomyAssessmentRequest invalidRequest = new AutonomyAssessmentRequest();
        invalidRequest.setPatientId(patientId);
        // Missing other required fields

        // When & Then
        mockMvc.perform(post("/api/v1/patients/{patientId}/autonomy", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors").exists());
    }

    // ====================================================================================
    // GET /api/v1/patients/{patientId}/autonomy
    // ====================================================================================

    @Test
    @DisplayName("Should return 200 with latest assessment when assessment exists")
    void shouldReturn200WithLatestAssessmentWhenAssessmentExists() throws Exception {
        // Given
        when(autonomyAssessmentService.getLatestAssessment(patientId))
                .thenReturn(Optional.of(assessmentResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}/autonomy", patientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assessmentId.toString()))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.hygieneLevel").value("INDEPENDENT"))
                .andExpect(jsonPath("$.medicationLevel").value("ASSISTED"))
                .andExpect(jsonPath("$.mobilityLevel").value("ASSISTED"))
                .andExpect(jsonPath("$.feedingLevel").value("INDEPENDENT"))
                .andExpect(jsonPath("$.overallScore").value(75))
                .andExpect(jsonPath("$.assessedBy").value("Dr. Sarah Johnson"))
                .andExpect(jsonPath("$.notes").value("Patient shows good progress in mobility"))
                .andExpect(jsonPath("$.assessedAt").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        verify(autonomyAssessmentService).getLatestAssessment(patientId);
    }

    @Test
    @DisplayName("Should return 404 when no assessment exists for patient")
    void shouldReturn404WhenNoAssessmentExistsForPatient() throws Exception {
        // Given - patient exists but has no assessments
        when(autonomyAssessmentService.getLatestAssessment(patientId))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}/autonomy", patientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(autonomyAssessmentService).getLatestAssessment(patientId);
    }

    @Test
    @DisplayName("Should return 404 when patient not found during latest assessment retrieval")
    void shouldReturn404WhenPatientNotFoundDuringLatestAssessmentRetrieval() throws Exception {
        // Given
        when(autonomyAssessmentService.getLatestAssessment(patientId))
                .thenThrow(new PatientNotFoundException(patientId));

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}/autonomy", patientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Patient Not Found"))
                .andExpect(jsonPath("$.detail").value("Patient profile not found with id: " + patientId))
                .andExpect(jsonPath("$.type").value("https://alzcare.com/errors/patient-not-found"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(autonomyAssessmentService).getLatestAssessment(patientId);
    }

    // ====================================================================================
    // GET /api/v1/patients/{patientId}/autonomy/history
    // ====================================================================================

    @Test
    @DisplayName("Should return 200 with assessment history list when assessments exist")
    void shouldReturn200WithAssessmentHistoryListWhenAssessmentsExist() throws Exception {
        // Given
        UUID assessmentId2 = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");
        AutonomyAssessmentResponse olderAssessment = new AutonomyAssessmentResponse();
        olderAssessment.setId(assessmentId2);
        olderAssessment.setPatientId(patientId);
        olderAssessment.setHygieneLevel(AutonomyLevel.ASSISTED);
        olderAssessment.setMedicationLevel(AutonomyLevel.DEPENDENT);
        olderAssessment.setMobilityLevel(AutonomyLevel.ASSISTED);
        olderAssessment.setFeedingLevel(AutonomyLevel.ASSISTED);
        olderAssessment.setOverallScore(50);
        olderAssessment.setAssessedBy("Dr. Michael Chen");
        olderAssessment.setNotes("Initial assessment - more support needed");
        olderAssessment.setAssessedAt(assessedAt.minusMonths(1));
        olderAssessment.setCreatedAt(createdAt.minusMonths(1));

        List<AutonomyAssessmentResponse> assessmentHistory = List.of(assessmentResponse, olderAssessment);

        when(autonomyAssessmentService.getAssessmentHistory(patientId))
                .thenReturn(assessmentHistory);

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}/autonomy/history", patientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assessmentId.toString()))
                .andExpect(jsonPath("$[0].patientId").value(patientId.toString()))
                .andExpect(jsonPath("$[0].hygieneLevel").value("INDEPENDENT"))
                .andExpect(jsonPath("$[0].overallScore").value(75))
                .andExpect(jsonPath("$[1].id").value(assessmentId2.toString()))
                .andExpect(jsonPath("$[1].hygieneLevel").value("ASSISTED"))
                .andExpect(jsonPath("$[1].overallScore").value(50))
                .andExpect(jsonPath("$[1].assessedBy").value("Dr. Michael Chen"));

        verify(autonomyAssessmentService).getAssessmentHistory(patientId);
    }

    @Test
    @DisplayName("Should return 200 with empty list when patient has no assessment history")
    void shouldReturn200WithEmptyListWhenPatientHasNoAssessmentHistory() throws Exception {
        // Given - patient exists but has no assessment history
        when(autonomyAssessmentService.getAssessmentHistory(patientId))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}/autonomy/history", patientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(autonomyAssessmentService).getAssessmentHistory(patientId);
    }

    @Test
    @DisplayName("Should return 404 when patient not found during history retrieval")
    void shouldReturn404WhenPatientNotFoundDuringHistoryRetrieval() throws Exception {
        // Given
        when(autonomyAssessmentService.getAssessmentHistory(patientId))
                .thenThrow(new PatientNotFoundException(patientId));

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}/autonomy/history", patientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Patient Not Found"))
                .andExpect(jsonPath("$.detail").value("Patient profile not found with id: " + patientId))
                .andExpect(jsonPath("$.type").value("https://alzcare.com/errors/patient-not-found"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(autonomyAssessmentService).getAssessmentHistory(patientId);
    }

    // ====================================================================================
    // Additional Edge Case Tests
    // ====================================================================================

    @Test
    @DisplayName("Should return 500 when patient ID is not a valid UUID (MethodArgumentTypeMismatchException)")
    void shouldReturn500WhenPatientIdIsNotValidUuid() throws Exception {
        // When & Then - MethodArgumentTypeMismatchException is not handled by GlobalExceptionHandler
        // so it falls through to the generic exception handler which returns 500
        mockMvc.perform(get("/api/v1/patients/invalid-uuid/autonomy")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal Server Error"));
    }

    @Test
    @DisplayName("Should handle all autonomy levels in request and response")
    void shouldHandleAllAutonomyLevelsInRequestAndResponse() throws Exception {
        // Given - request with all different autonomy levels
        AutonomyAssessmentRequest requestWithAllLevels = new AutonomyAssessmentRequest();
        requestWithAllLevels.setPatientId(patientId);
        requestWithAllLevels.setHygieneLevel(AutonomyLevel.INDEPENDENT);
        requestWithAllLevels.setMedicationLevel(AutonomyLevel.ASSISTED);
        requestWithAllLevels.setMobilityLevel(AutonomyLevel.DEPENDENT);
        requestWithAllLevels.setFeedingLevel(AutonomyLevel.INDEPENDENT);
        requestWithAllLevels.setAssessedBy("Nurse Jane Doe");
        requestWithAllLevels.setNotes("Comprehensive assessment");
        requestWithAllLevels.setAssessedAt(assessedAt);

        AutonomyAssessmentResponse responseWithAllLevels = new AutonomyAssessmentResponse();
        responseWithAllLevels.setId(assessmentId);
        responseWithAllLevels.setPatientId(patientId);
        responseWithAllLevels.setHygieneLevel(AutonomyLevel.INDEPENDENT);
        responseWithAllLevels.setMedicationLevel(AutonomyLevel.ASSISTED);
        responseWithAllLevels.setMobilityLevel(AutonomyLevel.DEPENDENT);
        responseWithAllLevels.setFeedingLevel(AutonomyLevel.INDEPENDENT);
        responseWithAllLevels.setOverallScore(42);
        responseWithAllLevels.setAssessedBy("Nurse Jane Doe");
        responseWithAllLevels.setNotes("Comprehensive assessment");
        responseWithAllLevels.setAssessedAt(assessedAt);
        responseWithAllLevels.setCreatedAt(createdAt);

        when(autonomyAssessmentService.createAssessment(eq(patientId), any(AutonomyAssessmentRequest.class)))
                .thenReturn(responseWithAllLevels);

        // When & Then
        mockMvc.perform(post("/api/v1/patients/{patientId}/autonomy", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithAllLevels)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hygieneLevel").value("INDEPENDENT"))
                .andExpect(jsonPath("$.medicationLevel").value("ASSISTED"))
                .andExpect(jsonPath("$.mobilityLevel").value("DEPENDENT"))
                .andExpect(jsonPath("$.feedingLevel").value("INDEPENDENT"))
                .andExpect(jsonPath("$.overallScore").value(42));
    }

    @Test
    @DisplayName("Should handle null notes in request")
    void shouldHandleNullNotesInRequest() throws Exception {
        // Given - request with null notes (optional field)
        AutonomyAssessmentRequest requestWithNullNotes = createValidRequest();
        requestWithNullNotes.setNotes(null);

        AutonomyAssessmentResponse responseWithNullNotes = createAssessmentResponse();
        responseWithNullNotes.setNotes(null);

        when(autonomyAssessmentService.createAssessment(eq(patientId), any(AutonomyAssessmentRequest.class)))
                .thenReturn(responseWithNullNotes);

        // When & Then
        mockMvc.perform(post("/api/v1/patients/{patientId}/autonomy", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithNullNotes)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notes").doesNotExist());
    }
}
