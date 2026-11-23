package com.alzcare.identity.controllers;

import com.alzcare.identity.dtos.requests.PatientCreateRequest;
import com.alzcare.identity.dtos.responses.PatientProfileResponse;
import com.alzcare.identity.dtos.requests.PatientUpdateRequest;
import com.alzcare.identity.enums.GenderEnum;
import com.alzcare.identity.enums.LanguageEnum;
import com.alzcare.identity.exceptions.PatientNotFoundException;
import com.alzcare.identity.services.PatientProfileService;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientProfileController.class)
@ContextConfiguration(classes = {PatientProfileController.class, GlobalExceptionHandler.class})
@DisplayName("PatientProfileController Tests")
class PatientProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatientProfileService patientProfileService;

    private PatientProfileResponse patientProfileResponse;
    private PatientCreateRequest validCreateRequest;
    private PatientUpdateRequest validUpdateRequest;
    private UUID patientId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        patientProfileResponse = new PatientProfileResponse();
        patientProfileResponse.setId(patientId);
        patientProfileResponse.setUserId(userId);
        patientProfileResponse.setFirstName("John");
        patientProfileResponse.setLastName("Doe");
        patientProfileResponse.setDateOfBirth(LocalDate.of(1980, 5, 15));
        patientProfileResponse.setGender(GenderEnum.MALE);
        patientProfileResponse.setPreferredLanguage(LanguageEnum.ENGLISH);
        patientProfileResponse.setPhotoUrl("https://example.com/photo.jpg");
        patientProfileResponse.setIsActive(true);
        patientProfileResponse.setCreatedAt(OffsetDateTime.now());
        patientProfileResponse.setUpdatedAt(OffsetDateTime.now());

        validCreateRequest = new PatientCreateRequest();
        validCreateRequest.setUserId(userId);
        validCreateRequest.setFirstName("John");
        validCreateRequest.setLastName("Doe");
        validCreateRequest.setDateOfBirth(LocalDate.of(1980, 5, 15));
        validCreateRequest.setGender(GenderEnum.MALE);
        validCreateRequest.setPreferredLanguage(LanguageEnum.ENGLISH);
        validCreateRequest.setPhotoUrl("https://example.com/photo.jpg");

        validUpdateRequest = new PatientUpdateRequest();
        validUpdateRequest.setFirstName("John");
        validUpdateRequest.setLastName("Doe");
        validUpdateRequest.setDateOfBirth(LocalDate.of(1980, 5, 15));
        validUpdateRequest.setGender(GenderEnum.MALE);
        validUpdateRequest.setPreferredLanguage(LanguageEnum.ENGLISH);
        validUpdateRequest.setPhotoUrl("https://example.com/photo.jpg");
        validUpdateRequest.setIsActive(true);
    }

    @Test
    @DisplayName("Should create patient and return 201 when request is valid")
    void shouldCreatePatientAndReturn201WhenRequestIsValid() throws Exception {
        // Given
        when(patientProfileService.createPatient(any(PatientCreateRequest.class)))
                .thenReturn(patientProfileResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.preferredLanguage").value("ENGLISH"))
                .andExpect(jsonPath("$.isActive").value(true));

        verify(patientProfileService, times(1)).createPatient(any(PatientCreateRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when create request has missing required fields")
    void shouldReturn400WhenCreateRequestHasMissingRequiredFields() throws Exception {
        // Given
        PatientCreateRequest invalidRequest = new PatientCreateRequest();
        // Missing all required fields

        // When & Then
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verify(patientProfileService, never()).createPatient(any(PatientCreateRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when create request has blank fields")
    void shouldReturn400WhenCreateRequestHasBlankFields() throws Exception {
        // Given
        PatientCreateRequest invalidRequest = new PatientCreateRequest();
        invalidRequest.setUserId(null);
        invalidRequest.setFirstName("");
        invalidRequest.setLastName("");
        invalidRequest.setDateOfBirth(LocalDate.of(1980, 5, 15));
        invalidRequest.setGender(GenderEnum.MALE);

        // When & Then
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(patientProfileService, never()).createPatient(any(PatientCreateRequest.class));
    }

    @Test
    @DisplayName("Should return 409 when patient already exists")
    void shouldReturn409WhenPatientAlreadyExists() throws Exception {
        // Given
        when(patientProfileService.createPatient(any(PatientCreateRequest.class)))
                .thenThrow(new IllegalArgumentException("Patient already exists with userId: " + userId));

        // When & Then
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isConflict());

        verify(patientProfileService, times(1)).createPatient(any(PatientCreateRequest.class));
    }

    @Test
    @DisplayName("Should return patient and 200 when found by userId")
    void shouldReturnPatientAnd200WhenFoundByUserId() throws Exception {
        // Given
        when(patientProfileService.getPatientByUserId(userId))
                .thenReturn(patientProfileResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.dateOfBirth").value("1980-05-15"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.preferredLanguage").value("ENGLISH"))
                .andExpect(jsonPath("$.photoUrl").value("https://example.com/photo.jpg"))
                .andExpect(jsonPath("$.isActive").value(true));

        verify(patientProfileService, times(1)).getPatientByUserId(userId);
    }

    @Test
    @DisplayName("Should return 404 when patient not found by userId")
    void shouldReturn404WhenPatientNotFoundByUserId() throws Exception {
        // Given
        UUID nonExistentUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655449999");
        when(patientProfileService.getPatientByUserId(nonExistentUserId))
                .thenThrow(new PatientNotFoundException(nonExistentUserId));

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{userId}", nonExistentUserId))
                .andExpect(status().isNotFound());

        verify(patientProfileService, times(1)).getPatientByUserId(nonExistentUserId);
    }

    @Test
    @DisplayName("Should update patient and return 200 when request is valid")
    void shouldUpdatePatientAndReturn200WhenRequestIsValid() throws Exception {
        // Given
        PatientProfileResponse updatedResponse = new PatientProfileResponse();
        updatedResponse.setId(patientId);
        updatedResponse.setUserId(userId);
        updatedResponse.setFirstName("Jane");
        updatedResponse.setLastName("Smith");
        updatedResponse.setDateOfBirth(LocalDate.of(1985, 8, 20));
        updatedResponse.setGender(GenderEnum.FEMALE);
        updatedResponse.setPreferredLanguage(LanguageEnum.FRENCH);
        updatedResponse.setPhotoUrl("https://example.com/new-photo.jpg");
        updatedResponse.setIsActive(false);
        updatedResponse.setCreatedAt(OffsetDateTime.now());
        updatedResponse.setUpdatedAt(OffsetDateTime.now());

        when(patientProfileService.updatePatient(eq(patientId), any(PatientUpdateRequest.class)))
                .thenReturn(updatedResponse);

        PatientUpdateRequest updateRequest = new PatientUpdateRequest();
        updateRequest.setFirstName("Jane");
        updateRequest.setLastName("Smith");
        updateRequest.setDateOfBirth(LocalDate.of(1985, 8, 20));
        updateRequest.setGender(GenderEnum.FEMALE);
        updateRequest.setPreferredLanguage(LanguageEnum.FRENCH);
        updateRequest.setPhotoUrl("https://example.com/new-photo.jpg");
        updateRequest.setIsActive(false);

        // When & Then
        mockMvc.perform(put("/api/v1/patients/{id}", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.gender").value("FEMALE"))
                .andExpect(jsonPath("$.preferredLanguage").value("FRENCH"))
                .andExpect(jsonPath("$.isActive").value(false));

        verify(patientProfileService, times(1)).updatePatient(eq(patientId), any(PatientUpdateRequest.class));
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent patient")
    void shouldReturn404WhenUpdatingNonExistentPatient() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(patientProfileService.updatePatient(eq(nonExistentId), any(PatientUpdateRequest.class)))
                .thenThrow(new PatientNotFoundException(nonExistentId));

        // When & Then
        mockMvc.perform(put("/api/v1/patients/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest)))
                .andExpect(status().isNotFound());

        verify(patientProfileService, times(1)).updatePatient(eq(nonExistentId), any(PatientUpdateRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when update request has missing required fields")
    void shouldReturn400WhenUpdateRequestHasMissingRequiredFields() throws Exception {
        // Given
        PatientUpdateRequest invalidRequest = new PatientUpdateRequest();
        // Missing all required fields

        // When & Then
        mockMvc.perform(put("/api/v1/patients/{id}", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verify(patientProfileService, never()).updatePatient(any(UUID.class), any(PatientUpdateRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when update request has null isActive")
    void shouldReturn400WhenUpdateRequestHasNullIsActive() throws Exception {
        // Given
        PatientUpdateRequest invalidRequest = new PatientUpdateRequest();
        invalidRequest.setFirstName("John");
        invalidRequest.setLastName("Doe");
        invalidRequest.setDateOfBirth(LocalDate.of(1980, 5, 15));
        invalidRequest.setGender(GenderEnum.MALE);
        invalidRequest.setIsActive(null);

        // When & Then
        mockMvc.perform(put("/api/v1/patients/{id}", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(patientProfileService, never()).updatePatient(any(UUID.class), any(PatientUpdateRequest.class));
    }
}
