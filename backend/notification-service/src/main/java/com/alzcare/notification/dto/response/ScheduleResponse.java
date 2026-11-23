package com.alzcare.notification.dto.response;

import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationPriority;
import com.alzcare.notification.enums.NotificationType;
import com.alzcare.notification.enums.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for notification schedule data.
 * <p>Returns complete schedule information including configuration,
 * execution metrics, and current status.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {

    private String id;
    private String name;
    private String description;
    private String targetRole;
    private List<String> targetUserIds;
    private String titleTemplate;
    private String messageTemplate;
    private NotificationType type;
    private NotificationPriority priority;
    private List<NotificationChannel> channels;
    private ScheduleType scheduleType;
    private String cronExpression;
    private Integer intervalMinutes;
    private Instant startDate;
    private Instant endDate;
    private String timezone;
    private boolean active;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastExecutedAt;
    private Integer executionCount;
    private Instant nextExecutionTime;
}
