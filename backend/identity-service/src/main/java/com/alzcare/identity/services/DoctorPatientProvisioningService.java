package com.alzcare.identity.services;

import com.alzcare.identity.dtos.requests.DoctorCreatePatientRequest;
import com.alzcare.identity.dtos.requests.UserCreateRequest;
import com.alzcare.identity.dtos.responses.DoctorCreatesPatientResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * PDF workflow: POST /doctors/{doctorId}/patients/create — create patient user + profile + doctor assignment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorPatientProvisioningService {

    private final KeycloakAdminService keycloakAdminService;
    private final RestTemplate restTemplate;

    @Value("${alzcare.care-team.base-url:http://localhost:8080}")
    private String careTeamBaseUrl;

    /**
     * @param authorization the caller's {@code Authorization} header. It is forwarded to
     *                      care-team, which only lets a doctor create assignments for
     *                      themselves (or an admin for anyone).
     */
    public DoctorCreatesPatientResponse provision(String doctorId, DoctorCreatePatientRequest req,
                                                  String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new IllegalArgumentException("Missing Authorization header");
        }
        String password = req.getPassword();
        String generatedPassword = null;
        if (password == null || password.isBlank()) {
            generatedPassword = generatePatientPassword();
            password = generatedPassword;
        }

        UserCreateRequest userReq = UserCreateRequest.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(password)
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .role(UserCreateRequest.UserRole.PATIENT)
                .emailVerified(Boolean.TRUE.equals(req.getEmailVerified()))
                .enabled(true)
                .dateOfBirth(req.getDateOfBirth())
                .gender(req.getGender())
                .phone(req.getPhone())
                .address(req.getAddress())
                .emergencyContact(req.getEmergencyContact())
                .build();

        String patientUserId = keycloakAdminService.createUser(userReq);

        String url = careTeamBaseUrl.replaceAll("/$", "")
                + "/api/v1/care-team/doctors/" + doctorId + "/patients/create";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                Map.of("patientId", patientUserId),
                headers
        );
        ResponseEntity<Map> assignmentResponse;
        try {
            assignmentResponse = restTemplate.postForEntity(url, entity, Map.class);
        } catch (ResourceAccessException e) {
            log.error("Care-team unreachable at {}: {}", url, e.getMessage());
            rollbackUser(patientUserId);
            throw new IllegalStateException(
                    "Care-team is unreachable at " + url
                            + ". Start care-team-service (and MySQL), ensure alzcare.care-team.base-url points to gateway (8080) or care-team (8008).",
                    e);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Care-team assignment HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            rollbackUser(patientUserId);
            throw new IllegalStateException(
                    "Care-team rejected doctor assignment (" + e.getStatusCode() + "): "
                            + e.getResponseBodyAsString(),
                    e);
        } catch (RestClientException e) {
            log.error("Care-team call failed: {}", e.getMessage());
            rollbackUser(patientUserId);
            throw new IllegalStateException("Care-team doctor assignment failed: " + e.getMessage(), e);
        }
        if (!assignmentResponse.getStatusCode().is2xxSuccessful() || assignmentResponse.getBody() == null) {
            throw new IllegalStateException("Care-team doctor assignment failed: " + assignmentResponse.getStatusCode());
        }
        Map<String, Object> body = assignmentResponse.getBody();
        String assignmentId = body.get("id") != null ? body.get("id").toString() : null;
        String status = body.get("status") != null ? body.get("status").toString() : null;

        try {
            String profileUrl = careTeamBaseUrl.replaceAll("/$", "")
                    + "/api/v1/care-team/patients/" + patientUserId;
            java.util.HashMap<String, String> profileBody = new java.util.HashMap<>();
            profileBody.put("fullName", req.getFirstName() + " " + req.getLastName());
            profileBody.put("dateOfBirth", req.getDateOfBirth());
            if (req.getAddress() != null && !req.getAddress().isBlank()) {
                profileBody.put("address", req.getAddress());
            }
            if (req.getDiagnosisStage() != null && !req.getDiagnosisStage().isBlank()) {
                profileBody.put("diagnosisStage", req.getDiagnosisStage());
            }
            HttpEntity<Map<String, String>> putEntity = new HttpEntity<>(profileBody, headers);
            restTemplate.exchange(profileUrl, HttpMethod.PUT, putEntity, Void.class);
        } catch (Exception e) {
            log.warn("Could not upsert care profile for patient {}: {}", patientUserId, e.getMessage());
        }

        return DoctorCreatesPatientResponse.builder()
                .patientUserId(patientUserId)
                .doctorId(doctorId)
                .assignmentId(assignmentId)
                .assignmentStatus(status)
                .generatedPassword(generatedPassword)
                .build();
    }

    /** Compensating action: don't leave an orphan Keycloak account when the assignment is rejected. */
    private void rollbackUser(String patientUserId) {
        try {
            keycloakAdminService.deleteUser(patientUserId);
            log.info("Rolled back Keycloak user {} after failed doctor assignment", patientUserId);
        } catch (RuntimeException ex) {
            log.error("Could not roll back Keycloak user {}: {}", patientUserId, ex.getMessage());
        }
    }

    private static String generatePatientPassword() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "Patient1!" + suffix;
    }
}
