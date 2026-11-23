package com.alzcare.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * RabbitMQ event payload for notification messages.
 * <p>Represents the complete event envelope sent by safety-alert-engine
 * when alerts are created or escalations occur.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {

    /**
     * Unique identifier for this event (UUID).
     */
    private String eventId;

    /**
     * Type of event: "AlertCreated", "NotificationRequested", "EscalationNotice".
     */
    private String eventType;

    /**
     * Source service that generated this event (e.g., "safety-alert-engine").
     */
    private String source;

    /**
     * Timestamp when the event was generated.
     */
    private Instant timestamp;

    /**
     * The actual notification payload containing recipient and message details.
     */
    private NotificationPayload payload;
}
