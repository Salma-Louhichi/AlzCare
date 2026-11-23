package com.alzcare.notification.dto.request;

import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating or updating a notification template.
 * <p>Template ID serves as the unique code for the template.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateRequest {

    @NotBlank(message = "Template ID is required")
    private String id;

    @NotBlank(message = "Template name is required")
    private String name;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotNull(message = "Notification channel is required")
    private NotificationChannel channel;

    private String subjectTemplate;

    @NotBlank(message = "Content template is required")
    private String contentTemplate;

    private String description;

    @Builder.Default
    private String language = "en";

    private List<String> variables;
}
