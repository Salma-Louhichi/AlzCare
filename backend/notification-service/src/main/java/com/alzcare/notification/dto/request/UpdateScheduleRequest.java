package com.alzcare.notification.dto.request;

import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationPriority;
import com.alzcare.notification.enums.NotificationType;
import com.alzcare.notification.enums.ScheduleType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.util.List;

/**
 * Request DTO for updating an existing notification schedule.
 * <p>Defines all parameters that can be updated for a scheduled notification rule.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateScheduleRequest {

    @NotBlank(message = "Schedule name is required")
    private String name;

    private String description;

    @NotNull(message = "Target role is required")
    private String targetRole;

    private List<String> targetUserIds;

    @NotBlank(message = "Title template is required")
    private String titleTemplate;

    @NotBlank(message = "Message template is required")
    private String messageTemplate;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    private NotificationPriority priority;

    @NotEmpty(message = "At least one notification channel must be provided")
    private List<NotificationChannel> channels;

    @NotNull(message = "Schedule type is required")
    private ScheduleType scheduleType;

    private String cronExpression;

    private Integer intervalMinutes;

    @NotNull(message = "Start date is required")
    private Instant startDate;

    private Instant endDate;

    private String timezone;

    private boolean active;

    /**
     * Validates that cron expression is provided and valid when schedule type is CRON.
     *
     * @return true if valid, false otherwise
     */
    @AssertTrue(message = "Cron expression is required and must be valid when schedule type is CRON")
    public boolean isCronExpressionValid() {
        if (scheduleType != ScheduleType.CRON) {
            return true;
        }
        if (cronExpression == null || cronExpression.isBlank()) {
            return false;
        }
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates that interval minutes is provided and positive when schedule type is INTERVAL.
     *
     * @return true if valid, false otherwise
     */
    @AssertTrue(message = "Interval minutes must be greater than 0 when schedule type is INTERVAL")
    public boolean isIntervalMinutesValid() {
        if (scheduleType != ScheduleType.INTERVAL) {
            return true;
        }
        return intervalMinutes != null && intervalMinutes > 0;
    }
}
