package com.alzcare.careteam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * JSON body for POST /api/v1/notifications (notification-service).
 * Duplicated here to avoid a hard Maven dependency between microservices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreatePayload {

    private String recipientId;
    private String type;
    private String channel;
    private String priority;
    private String title;
    private String message;
    private Map<String, Object> metadata;
}
