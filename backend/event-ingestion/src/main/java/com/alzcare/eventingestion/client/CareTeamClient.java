package com.alzcare.eventingestion.client;

import com.alzcare.eventingestion.config.ServiceProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

/** Who looks after a patient (care-team-service), and the patient's name (identity-service). */
@Component
public class CareTeamClient {

    private static final String REGISTRATION = "event-ingestion";

    private final RestClient rest;
    private final ServiceProperties services;
    private final OAuth2AuthorizedClientManager clientManager;

    public CareTeamClient(RestClient platformRestClient, ServiceProperties services,
                          OAuth2AuthorizedClientManager serviceClientManager) {
        this.rest = platformRestClient;
        this.services = services;
        this.clientManager = serviceClientManager;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DoctorAssignment(String doctorId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CaregiverAssignment(String caregiverId, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PatientProfile(String firstName, String lastName) {
    }

    /** Active doctor of the patient; empty when none (care-team answers an error status then). */
    public Optional<String> activeDoctorId(String patientId) {
        try {
            DoctorAssignment a = rest.get()
                    .uri(services.careTeamUrl() + "/api/v1/care-team/patients/{id}/doctor", patientId)
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .retrieve().body(DoctorAssignment.class);
            return Optional.ofNullable(a).map(DoctorAssignment::doctorId);
        } catch (HttpStatusCodeException e) {
            return Optional.empty();
        }
    }

    /** Caregivers whose assignment to the patient is ACTIVE. */
    public List<String> activeCaregiverIds(String patientId) {
        CaregiverAssignment[] all = rest.get()
                .uri(services.careTeamUrl() + "/api/v1/care-team/patients/{id}/caregivers", patientId)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve().body(CaregiverAssignment[].class);
        if (all == null) {
            return List.of();
        }
        return List.of(all).stream()
                .filter(a -> "ACTIVE".equals(a.status()) && a.caregiverId() != null)
                .map(CaregiverAssignment::caregiverId)
                .toList();
    }

    /** "Firstname Lastname", or empty if identity-service has no profile. */
    public Optional<String> patientName(String patientId) {
        try {
            PatientProfile p = rest.get()
                    .uri(services.identityUrl() + "/api/v1/patients/{id}", patientId)
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .retrieve().body(PatientProfile.class);
            return Optional.ofNullable(p).map(x -> (x.firstName() + " " + x.lastName()).trim());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private String bearer() {
        OAuth2AuthorizedClient client = clientManager.authorize(
                OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION).principal("event-ingestion").build());
        if (client == null) {
            throw new IllegalStateException("Could not obtain a service token from Keycloak");
        }
        return "Bearer " + client.getAccessToken().getTokenValue();
    }
}
