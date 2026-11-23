package com.alzcare.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for notification template data.
 * <p>Returns template information including placeholders and configuration.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateResponse {

    private String id;
    private String name;
    private String type;
    private String channel;
    private String subjectTemplate;
    private String contentTemplate;
    private String description;
    private String language;
    private List<String> variables;
    private Instant createdAt;
    private Instant updatedAt;
}
