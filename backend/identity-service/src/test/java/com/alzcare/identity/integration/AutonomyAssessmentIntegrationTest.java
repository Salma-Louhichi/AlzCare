package com.alzcare.identity.integration;

import com.alzcare.identity.dtos.requests.AutonomyAssessmentRequest;
import com.alzcare.identity.dtos.requests.PatientCreateRequest;
import com.alzcare.identity.dtos.responses.AutonomyAssessmentResponse;
import com.alzcare.identity.dtos.responses.PatientProfileResponse;
import com.alzcare.identity.enums.AutonomyLevel;
import com.alzcare.identity.enums.GenderEnum;
import com.alzcare.identity.enums.LanguageEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full integration tests for Autonomy Assessment endpoints.
 * Tests the complete flow: Create Patient → Create Assessment → Get Latest → Get History.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Autonomy Assessment Full Integration Tests")
class AutonomyAssessmentIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private PatientCreateRequest createPatientRequest(UUID userId, String firstName, String lastName) {
        PatientCreateRequest request = new PatientCreateRequest();
        request.setUserId(userId);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setDateOfBirth(LocalDate.of(1950, 5, 15));
        request.setGender(GenderEnum.MALE);
        return request;
    }

    private AutonomyAssessmentRequest createAssessmentRequest(AutonomyLevel hygiene, AutonomyLevel medication, 
                                                               AutonomyLevel mobility, AutonomyLevel feeding,
                                                               UUID assessedBy, LocalDate assessmentDate) {
        AutonomyAssessmentRequest request = new AutonomyAssessmentRequest();
        request.setHygieneLevel(hygiene);
        request.setMedicationLevel(medication);
        request.setMobilityLevel(mobility);
        request.setFeedingLevel(feeding);
        request.setAssessedBy(assessedBy);
        request.setAssessmentDate(assessmentDate);
        return request;
    }

    @Test
    @DisplayName("Should create assessment and retrieve latest for patient")
    void shouldCreateAssessmentAndRetrieveLatestForPatient() {
        // Given - Create patient first
        UUID patientUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        PatientCreateRequest patientRequest = createPatientRequest(patientUserId, "Assessment", "Patient");
        patientRequest.setPreferredLanguage(LanguageEnum.ENGLISH);

        ResponseEntity<PatientProfileResponse> patientResponse = restTemplate.postForEntity(
                "/api/v1/patients",
                patientRequest,
                PatientProfileResponse.class
        );
        assertThat(patientResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID patientId = patientResponse.getBody().getId();

        // When - Create assessment
        UUID assessedById = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
        AutonomyAssessmentRequest assessmentRequest = createAssessmentRequest(
                AutonomyLevel.ASSISTED, AutonomyLevel.INDEPENDENT, AutonomyLevel.ASSISTED, AutonomyLevel.INDEPENDENT,
                assessedById, LocalDate.now()
        );
        assessmentRequest.setNotes("Initial assessment during integration test");

        ResponseEntity<AutonomyAssessmentResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/patients/" + patientId + "/autonomy",
                assessmentRequest,
                AutonomyAssessmentResponse.class
        );

        // Then - Verify assessment creation
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getPatientId()).isEqualTo(patientId);
        assertThat(createResponse.getBody().getHygieneLevel()).isEqualTo(AutonomyLevel.ASSISTED);
        assertThat(createResponse.getBody().getMedicationLevel()).isEqualTo(AutonomyLevel.INDEPENDENT);
        assertThat(createResponse.getBody().getAssessedBy()).isEqualTo(assessedById);
        assertThat(createResponse.getBody().getOverallScore()).isNotNull();

        // When - Get latest assessment
        ResponseEntity<AutonomyAssessmentResponse> latestResponse = restTemplate.getForEntity(
                "/api/v1/patients/" + patientId + "/autonomy",
                AutonomyAssessmentResponse.class
        );

        // Then - Verify latest assessment
        assertThat(latestResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(latestResponse.getBody()).isNotNull();
        assertThat(latestResponse.getBody().getPatientId()).isEqualTo(patientId);
        assertThat(latestResponse.getBody().getAssessedBy()).isEqualTo(assessedById);
    }

    @Test
    @DisplayName("Should return 404 when creating assessment for non-existent patient")
    void shouldReturn404WhenCreatingAssessmentForNonExistentPatient() {
        // Given - Non-existent patient ID
        UUID nonExistentPatientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440888");
        UUID assessedById = UUID.fromString("550e8400-e29b-41d4-a716-446655440011");

        AutonomyAssessmentRequest assessmentRequest = createAssessmentRequest(
                AutonomyLevel.ASSISTED, AutonomyLevel.INDEPENDENT, AutonomyLevel.ASSISTED, AutonomyLevel.INDEPENDENT,
                assessedById, LocalDate.now()
        );

        // When - Try to create assessment
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/patients/" + nonExistentPatientId + "/autonomy",
                assessmentRequest,
                String.class
        );

        // Then - Verify 404 response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return 404 when getting latest assessment for patient with no assessments")
    void shouldReturn404WhenGettingLatestAssessmentForPatientWithNoAssessments() {
        // Given - Create patient without any assessments
        UUID patientUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        PatientCreateRequest patientRequest = createPatientRequest(patientUserId, "No", "Assessment");
        patientRequest.setDateOfBirth(LocalDate.of(1940, 1, 1));
        patientRequest.setGender(GenderEnum.FEMALE);

        ResponseEntity<PatientProfileResponse> patientResponse = restTemplate.postForEntity(
                "/api/v1/patients",
                patientRequest,
                PatientProfileResponse.class
        );
        assertThat(patientResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID patientId = patientResponse.getBody().getId();

        // When - Try to get latest assessment
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/patients/" + patientId + "/autonomy",
                String.class
        );

        // Then - Verify 404 response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should retrieve assessment history for patient")
    void shouldRetrieveAssessmentHistoryForPatient() {
        // Given - Create patient
        UUID patientUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
        PatientCreateRequest patientRequest = createPatientRequest(patientUserId, "History", "Patient");
        patientRequest.setDateOfBirth(LocalDate.of(1955, 8, 20));

        ResponseEntity<PatientProfileResponse> patientResponse = restTemplate.postForEntity(
                "/api/v1/patients",
                patientRequest,
                PatientProfileResponse.class
        );
        assertThat(patientResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID patientId = patientResponse.getBody().getId();

        // Create multiple assessments
        UUID assessedBy1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440021");
        UUID assessedBy2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440022");
        UUID assessedBy3 = UUID.fromString("550e8400-e29b-41d4-a716-446655440023");

        AutonomyAssessmentRequest assessment1 = createAssessmentRequest(
                AutonomyLevel.DEPENDENT, AutonomyLevel.DEPENDENT, AutonomyLevel.DEPENDENT, AutonomyLevel.DEPENDENT,
                assessedBy1, LocalDate.now().minusDays(30)
        );
        assessment1.setNotes("Initial poor assessment");

        AutonomyAssessmentRequest assessment2 = createAssessmentRequest(
                AutonomyLevel.ASSISTED, AutonomyLevel.ASSISTED, AutonomyLevel.DEPENDENT, AutonomyLevel.ASSISTED,
                assessedBy2, LocalDate.now().minusDays(15)
        );
        assessment2.setNotes("Some improvement");

        AutonomyAssessmentRequest assessment3 = createAssessmentRequest(
                AutonomyLevel.INDEPENDENT, AutonomyLevel.INDEPENDENT, AutonomyLevel.ASSISTED, AutonomyLevel.INDEPENDENT,
                assessedBy3, LocalDate.now()
        );
        assessment3.setNotes("Significant improvement");

        restTemplate.postForEntity(
                "/api/v1/patients/" + patientId + "/autonomy",
                assessment1,
                AutonomyAssessmentResponse.class
        );
        restTemplate.postForEntity(
                "/api/v1/patients/" + patientId + "/autonomy",
                assessment2,
                AutonomyAssessmentResponse.class
        );
        restTemplate.postForEntity(
                "/api/v1/patients/" + patientId + "/autonomy",
                assessment3,
                AutonomyAssessmentResponse.class
        );

        // When - Get assessment history
        ResponseEntity<AutonomyAssessmentResponse[]> historyResponse = restTemplate.getForEntity(
                "/api/v1/patients/" + patientId + "/autonomy/history",
                AutonomyAssessmentResponse[].class
        );

        // Then - Verify history
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyResponse.getBody()).isNotNull();
        assertThat(historyResponse.getBody()).hasSize(3);

        List<AutonomyAssessmentResponse> history = List.of(historyResponse.getBody());
        // Verify overall scores improve over time
        assertThat(history.get(0).getOverallScore()).isLessThan(history.get(2).getOverallScore());
    }

    @Test
    @DisplayName("Should calculate correct overall score for assessment")
    void shouldCalculateCorrectOverallScoreForAssessment() {
        // Given - Create patient
        UUID patientUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440004");
        PatientCreateRequest patientRequest = createPatientRequest(patientUserId, "Score", "Patient");
        patientRequest.setDateOfBirth(LocalDate.of(1948, 3, 10));
        patientRequest.setGender(GenderEnum.FEMALE);

        ResponseEntity<PatientProfileResponse> patientResponse = restTemplate.postForEntity(
                "/api/v1/patients",
                patientRequest,
                PatientProfileResponse.class
        );
        assertThat(patientResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID patientId = patientResponse.getBody().getId();

        // Create assessment with all INDEPENDENT = 100%
        UUID assessedById = UUID.fromString("550e8400-e29b-41d4-a716-446655440030");
        AutonomyAssessmentRequest assessmentRequest = createAssessmentRequest(
                AutonomyLevel.INDEPENDENT, AutonomyLevel.INDEPENDENT, AutonomyLevel.INDEPENDENT, AutonomyLevel.INDEPENDENT,
                assessedById, LocalDate.now()
        );

        // When - Create assessment
        ResponseEntity<AutonomyAssessmentResponse> response = restTemplate.postForEntity(
                "/api/v1/patients/" + patientId + "/autonomy",
                assessmentRequest,
                AutonomyAssessmentResponse.class
        );

        // Then - Verify 100% score
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getOverallScore()).isEqualTo(100);

        // Create assessment with all DEPENDENT = 33%
        AutonomyAssessmentRequest lowAssessmentRequest = createAssessmentRequest(
                AutonomyLevel.DEPENDENT, AutonomyLevel.DEPENDENT, AutonomyLevel.DEPENDENT, AutonomyLevel.DEPENDENT,
                assessedById, LocalDate.now().minusDays(1)
        );

        ResponseEntity<AutonomyAssessmentResponse> lowResponse = restTemplate.postForEntity(
                "/api/v1/patients/" + patientId + "/autonomy",
                lowAssessmentRequest,
                AutonomyAssessmentResponse.class
        );

        assertThat(lowResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(lowResponse.getBody().getOverallScore()).isEqualTo(33);
    }

    @Test
    @DisplayName("Should return empty history for patient with no assessments")
    void shouldReturnEmptyHistoryForPatientWithNoAssessments() {
        // Given - Create patient without assessments
        UUID patientUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440005");
        PatientCreateRequest patientRequest = createPatientRequest(patientUserId, "Empty", "History");
        patientRequest.setDateOfBirth(LocalDate.of(1942, 6, 15));
        patientRequest.setGender(GenderEnum.MALE);

        ResponseEntity<PatientProfileResponse> patientResponse = restTemplate.postForEntity(
                "/api/v1/patients",
                patientRequest,
                PatientProfileResponse.class
        );
        assertThat(patientResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID patientId = patientResponse.getBody().getId();

        // When - Get history
        ResponseEntity<AutonomyAssessmentResponse[]> historyResponse = restTemplate.getForEntity(
                "/api/v1/patients/" + patientId + "/autonomy/history",
                AutonomyAssessmentResponse[].class
        );

        // Then - Verify empty array
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyResponse.getBody()).isNotNull();
        assertThat(historyResponse.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Should return 404 when getting history for non-existent patient")
    void shouldReturn404WhenGettingHistoryForNonExistentPatient() {
        // Given - Non-existent patient ID
        UUID nonExistentPatientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440777");

        // When - Try to get history
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/patients/" + nonExistentPatientId + "/autonomy/history",
                String.class
        );

        // Then - Verify 404 response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return 400 when creating assessment with invalid data")
    void shouldReturn400WhenCreatingAssessmentWithInvalidData() {
        // Given - Create patient first
        UUID patientUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440006");
        PatientCreateRequest patientRequest = createPatientRequest(patientUserId, "Invalid", "Assessment");
        patientRequest.setDateOfBirth(LocalDate.of(1950, 1, 1));

        ResponseEntity<PatientProfileResponse> patientResponse = restTemplate.postForEntity(
                "/api/v1/patients",
                patientRequest,
                PatientProfileResponse.class
        );
        assertThat(patientResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID patientId = patientResponse.getBody().getId();

        // Invalid request - missing required fields (send as raw JSON with only one field)
        String invalidJson = "{\"hygieneLevel\":\"INDEPENDENT\"}";

        // When - Try to create with invalid data
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/patients/" + patientId + "/autonomy",
                invalidJson,
                String.class
        );

        // Then - Verify bad request
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
