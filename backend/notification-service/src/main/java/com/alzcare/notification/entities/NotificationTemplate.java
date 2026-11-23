package com.alzcare.notification.entities;

import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB document representing a notification template.
 * <p>Stores reusable templates for different types of notifications with
 * placeholder support for dynamic content generation.</p>
 */
@Document(collection = "notification_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {

    @Id
    private String id;  // Template code like "ALERT_CRITICAL"

    @Indexed
    private String name;  // Human-readable name

    private NotificationType type;

    @Indexed
    private NotificationChannel channel;

    private String subjectTemplate;  // Subject with placeholders like {{patientName}}

    private String contentTemplate;  // Body with placeholders

    private String description;  // When to use this template

    private String language;  // en, fr, ar

    private List<String> variables;  // List of variable names like ["patientName", "alertType"]

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
