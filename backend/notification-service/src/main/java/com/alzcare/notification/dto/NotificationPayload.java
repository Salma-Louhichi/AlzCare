package com.alzcare.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Notification payload containing the actual content to be delivered.
 * <p>This is the core data structure that contains recipient information,
 * severity level, message content, and delivery channel preferences.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPayload {

    /**
     * Reference to the alert ID from safety-alert-engine (if applicable).
     */
    private String alertId;

    /**
     * Patient ID associated with this notification.
     */
    private String patientId;

    /**
     * User ID of the recipient who should receive this notification.
     */
    private String recipientId;

    /**
     * Severity level: CRITICAL, HIGH, MEDIUM, or LOW.
     */
    private String severity;

    /**
     * Rule code that triggered this notification (e.g., "FALL_DETECTED", "WANDERING").
     */
    private String ruleCode;

    /**
     * Pre-formatted message content for the notification.
     */
    private String message;

    /**
     * Delivery channel: SMS, EMAIL, PUSH, or IN_APP.
     */
    private String channel;

    /**
     * Additional metadata for the notification (flexible key-value data).
     */
    private Map<String, Object> metadata;
}
