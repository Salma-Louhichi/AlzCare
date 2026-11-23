package com.alzcare.identity.integration;

import com.alzcare.identity.dtos.requests.PatientCreateRequest;
import com.alzcare.identity.dtos.requests.PatientUpdateRequest;
import com.alzcare.identity.dtos.responses.PatientProfileResponse;
import com.alzcare.identity.enums.GenderEnum;
import com.alzcare.identity.enums.LanguageEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full integration tests for Patient Profile endpoints.
 * Tests the complete flow: Create → Retrieve → Update using Testcontainers PostgreSQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Patient Profile Full Integration Tests")
class PatientProfileIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private PatientCreateRequest createPatientRequest(UUID userId, String firstName, String lastName) {
        PatientCreateRequest request = new PatientCreateRequest();
        request.setUserId(userId);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setDateOfBirth(LocalDate.of(1960, 3, 15));
        request.setGender(GenderEnum.MALE);
        return request;
    }

    @Test
    @DisplayName("Should create patient and retrieve by User ID")
    void shouldCreatePatientAndRetrieveByUserId() {
        // Given - Create request
        UUID testUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        PatientCreateRequest createRequest = createPatientRequest(testUserId, "Integration", "Test");
        createRequest.setPreferredLanguage(LanguageEnum.ENGLISH);

        // When - Create patient
        ResponseEntity<PatientProfileResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/patients",
                createRequest,
                PatientProfileResponse.class
        );

        // Then - Verify creation
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getUserId()).isEqualTo(testUserId);
        assertThat(createResponse.getBody().getFirstName()).isEqualTo("Integration");
        assertThat(createResponse.getBody().getId()).isNotNull();

        // When - Retrieve by User ID
        ResponseEntity<PatientProfileResponse> getResponse = restTemplate.getForEntity(
                "/api/v1/patients/" + testUserId,
                PatientProfileResponse.class
        );

        // Then - Verify retrieval
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getUserId()).isEqualTo(testUserId);
        assertThat(getResponse.getBody().getFirstName()).isEqualTo("Integration");
    }

    @Test
    @DisplayName("Should return 404 when patient not found by User ID")
    void shouldReturn404WhenPatientNotFoundByUserId() {
        // When - Try to retrieve non-existent patient
        UUID nonExistentUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655449999");
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/patients/" + nonExistentUserId,
                String.class
        );

        // Then - Verify 404 response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return 409 when creating patient with duplicate User ID")
    void shouldReturn409WhenCreatingPatientWithDuplicateUserId() {
        // Given - Create first patient
        UUID duplicateUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        PatientCreateRequest createRequest = createPatientRequest(duplicateUserId, "First", "Patient");
        createRequest.setDateOfBirth(LocalDate.of(1955, 7, 20));
        createRequest.setGender(GenderEnum.FEMALE);

        ResponseEntity<PatientProfileResponse> firstResponse = restTemplate.postForEntity(
                "/api/v1/patients",
                createRequest,
                PatientProfileResponse.class
        );
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // When - Try to create second patient with same User ID
        PatientCreateRequest duplicateRequest = createPatientRequest(duplicateUserId, "Second", "Patient");
        duplicateRequest.setDateOfBirth(LocalDate.of(1960, 1, 1));
        duplicateRequest.setGender(GenderEnum.MALE);

        ResponseEntity<String> duplicateResponse = restTemplate.postForEntity(
                "/api/v1/patients",
                duplicateRequest,
                String.class
        );

        // Then - Verify conflict response
        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Should update patient profile successfully")
    void shouldUpdatePatientProfileSuccessfully() {
        // Given - Create patient first
        UUID updateUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
        PatientCreateRequest createRequest = createPatientRequest(updateUserId, "Original", "Name");
        createRequest.setDateOfBirth(LocalDate.of(1950, 5, 10));
        createRequest.setPreferredLanguage(LanguageEnum.ENGLISH);

        ResponseEntity<PatientProfileResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/patients",
                createRequest,
                PatientProfileResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String patientId = createResponse.getBody().getId().toString();

        // When - Update patient
        PatientUpdateRequest updateRequest = new PatientUpdateRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("NameChanged");
        updateRequest.setDateOfBirth(LocalDate.of(1950, 5, 10));
        updateRequest.setGender(GenderEnum.MALE);
        updateRequest.setPreferredLanguage(LanguageEnum.FRENCH);
        updateRequest.setCulturalContext("Updated cultural context");
        updateRequest.setIsActive(true);

        ResponseEntity<PatientProfileResponse> updateResponse = restTemplate.exchange(
                "/api/v1/patients/" + patientId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                PatientProfileResponse.class
        );

        // Then - Verify update
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isNotNull();
        assertThat(updateResponse.getBody().getFirstName()).isEqualTo("Updated");
        assertThat(updateResponse.getBody().getLastName()).isEqualTo("NameChanged");
        assertThat(updateResponse.getBody().getPreferredLanguage()).isEqualTo(LanguageEnum.FRENCH);
        assertThat(updateResponse.getBody().getCulturalContext()).isEqualTo("Updated cultural context");
        // User ID should remain unchanged
        assertThat(updateResponse.getBody().getUserId()).isEqualTo(updateUserId);
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent patient")
    void shouldReturn404WhenUpdatingNonExistentPatient() {
        // Given - Update request for non-existent patient
        String nonExistentId = "550e8400-e29b-41d4-a716-446655440999";
        PatientUpdateRequest updateRequest = new PatientUpdateRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
        updateRequest.setDateOfBirth(LocalDate.of(1950, 5, 10));
        updateRequest.setGender(GenderEnum.MALE);
        updateRequest.setIsActive(true);

        // When - Try to update
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/patients/" + nonExistentId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                String.class
        );

        // Then - Verify 404 response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return 400 when creating patient with invalid data")
    void shouldReturn400WhenCreatingPatientWithInvalidData() {
        // Given - Invalid request (missing required fields)
        PatientCreateRequest invalidRequest = new PatientCreateRequest();
        invalidRequest.setUserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440004"));
        // Missing firstName
        invalidRequest.setLastName("Test");
        invalidRequest.setDateOfBirth(LocalDate.of(1950, 5, 10));
        invalidRequest.setGender(GenderEnum.MALE);

        // When - Try to create
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/patients",
                invalidRequest,
                String.class
        );

        // Then - Verify bad request response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should create patient with all fields and verify response")
    void shouldCreatePatientWithAllFieldsAndVerifyResponse() {
        // Given - Complete create request
        UUID completeUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440005");
        PatientCreateRequest createRequest = new PatientCreateRequest();
        createRequest.setUserId(completeUserId);
        createRequest.setFirstName("Complete");
        createRequest.setLastName("Patient");
        createRequest.setDateOfBirth(LocalDate.of(1945, 12, 25));
        createRequest.setGender(GenderEnum.FEMALE);
        createRequest.setPreferredLanguage(LanguageEnum.ARABIC);
        createRequest.setCulturalContext("Requires Arabic-speaking caregiver");
        createRequest.setPhotoUrl("https://example.com/photos/patient.jpg");

        // When - Create patient
        ResponseEntity<PatientProfileResponse> response = restTemplate.postForEntity(
                "/api/v1/patients",
                createRequest,
                PatientProfileResponse.class
        );

        // Then - Verify all fields are stored correctly
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PatientProfileResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getUserId()).isEqualTo(completeUserId);
        assertThat(body.getFirstName()).isEqualTo("Complete");
        assertThat(body.getLastName()).isEqualTo("Patient");
        assertThat(body.getDateOfBirth()).isEqualTo(LocalDate.of(1945, 12, 25));
        assertThat(body.getGender()).isEqualTo(GenderEnum.FEMALE);
        assertThat(body.getPreferredLanguage()).isEqualTo(LanguageEnum.ARABIC);
        assertThat(body.getCulturalContext()).isEqualTo("Requires Arabic-speaking caregiver");
        assertThat(body.getPhotoUrl()).isEqualTo("https://example.com/photos/patient.jpg");
        assertThat(body.getIsActive()).isTrue();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getCreatedAt()).isNotNull();
        assertThat(body.getUpdatedAt()).isNotNull();
    }
}
