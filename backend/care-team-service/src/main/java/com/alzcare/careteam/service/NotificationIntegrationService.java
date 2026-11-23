package com.alzcare.careteam.service;

import com.alzcare.careteam.dto.NotificationCreatePayload;
import com.alzcare.careteam.entity.ChecklistItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Inter-service call to notification-service (Sprint 1: communication entre microservices, FR7).
 */
@Service
@Slf4j
public class NotificationIntegrationService {

    private final RestTemplate restTemplate;

    @Value("${alzcare.notifications.enabled:true}")
    private boolean enabled;

    @Value("${alzcare.notifications.base-url:http://localhost:8004}")
    private String notificationBaseUrl;

    public NotificationIntegrationService(RestTemplate careTeamRestTemplate) {
        this.restTemplate = careTeamRestTemplate;
    }

    public void notifyDoctorChecklistItemCompleted(ChecklistItem item, String completedByUserId) {
        if (!enabled || item.getDoctorId() == null || item.getDoctorId().isBlank()) {
            return;
        }
        try {
            NotificationCreatePayload payload = NotificationCreatePayload.builder()
                    .recipientId(item.getDoctorId())
                    .type("SYSTEM")
                    .channel("IN_APP")
                    .priority("NORMAL")
                    .title("Checklist task completed")
                    .message(String.format(
                            "Caregiver %s completed task for patient %s: %s",
                            completedByUserId != null ? completedByUserId : "unknown",
                            item.getPatientId(),
                            truncate(item.getDescription(), 120)))
                    .metadata(Map.of(
                            "checklistItemId", item.getId().toString(),
                            "patientId", item.getPatientId(),
                            "source", "care-team-service"
                    ))
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(
                    notificationBaseUrl + "/api/v1/notifications",
                    new HttpEntity<>(payload, headers),
                    Void.class);
            log.debug("Sent checklist completion notification to doctor {}", item.getDoctorId());
        } catch (Exception e) {
            log.warn("Could not reach notification-service (checklist still saved): {}", e.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
