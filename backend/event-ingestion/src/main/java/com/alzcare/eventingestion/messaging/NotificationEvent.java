package com.alzcare.eventingestion.messaging;

import java.time.Instant;
import java.util.Map;

/**
 * Message consumed by notification-service from the "alert.notifications" exchange.
 * Field names mirror com.alzcare.notification.dto.NotificationEvent / NotificationPayload.
 */
public record NotificationEvent(String eventId, String eventType, String source, Instant timestamp,
                                Payload payload) {

    public record Payload(String alertId, String patientId, String recipientId, String severity,
                          String ruleCode, String message, String channel, Map<String, Object> metadata) {
    }
}
