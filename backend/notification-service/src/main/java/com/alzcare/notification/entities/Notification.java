package com.alzcare.notification.entities;

import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationPriority;
import com.alzcare.notification.enums.NotificationStatus;
import com.alzcare.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * MongoDB document representing a notification.
 * <p>Stores all notification data including recipient, type, channel, status,
 * and delivery tracking information.</p>
 */
@Document(collection = "notifications")
@CompoundIndex(name = "recipient_status_idx", def = "{'recipientId': 1, 'status': 1}")
@CompoundIndex(name = "status_retry_idx", def = "{'status': 1, 'retryCount': 1}")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;  // MongoDB uses String for ID

    @Indexed
    private String recipientId;  // User ID (UUID as String)

    private NotificationType type;

    private NotificationChannel channel;

    @Indexed
    private NotificationStatus status;

    private NotificationPriority priority;

    private String title;

    private String message;

    private String templateId;  // Reference to template used

    private Instant scheduledAt;  // null = immediate

    private Instant sentAt;

    private Instant deliveredAt;

    private Instant readAt;

    @Builder.Default
    private Integer retryCount = 0;

    private String errorMessage;  // If failed

    private Map<String, Object> metadata;  // Flexible JSON data (alertId, patientId, etc.)

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
