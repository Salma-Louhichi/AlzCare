package com.alzcare.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for notification data.
 * <p>Returns complete notification information including delivery tracking.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String id;
    private String recipientId;
    private String type;
    private String channel;
    private String status;
    private String priority;
    private String title;
    private String message;
    private String templateId;
    private Instant scheduledAt;
    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private Integer retryCount;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
