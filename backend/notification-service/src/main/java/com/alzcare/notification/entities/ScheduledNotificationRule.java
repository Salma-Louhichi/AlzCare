package com.alzcare.notification.entities;

import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationPriority;
import com.alzcare.notification.enums.NotificationType;
import com.alzcare.notification.enums.ScheduleType;
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
 * MongoDB document representing a scheduled notification rule.
 * <p>Defines rules for sending notifications on a recurring or scheduled basis,
 * supporting interval-based, cron-based, and one-time scheduling mechanisms.
 * Rules can target specific user roles or individual users with customizable
 * message templates and delivery channels.</p>
 */
@Document(collection = "scheduled_notification_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledNotificationRule {

    @Id
    private String id;

    /**
     * Display name for the rule.
     */
    private String name;

    /**
     * Admin description explaining the purpose of this rule.
     */
    private String description;

    /**
     * Target role for notification delivery: PATIENT, CAREGIVER, DOCTOR, ALL.
     */
    @Indexed
    private String targetRole;

    /**
     * Optional list of specific user IDs to target.
     * If null or empty, all users matching the targetRole will receive notifications.
     */
    private List<String> targetUserIds;

    /**
     * Title template with placeholders, e.g., "Hello {{userName}}".
     */
    private String titleTemplate;

    /**
     * Message body template with placeholders for dynamic content.
     */
    private String messageTemplate;

    /**
     * Type of notification (ALERT, REMINDER, SYSTEM, EMERGENCY).
     */
    @Indexed
    private NotificationType type;

    /**
     * Priority level for the notification (LOW, NORMAL, HIGH, CRITICAL).
     */
    private NotificationPriority priority;

    /**
     * List of channels through which the notification should be sent.
     */
    private List<NotificationChannel> channels;

    // Scheduling

    /**
     * Type of scheduling mechanism: INTERVAL, CRON, ONE_TIME.
     */
    @Indexed
    private ScheduleType scheduleType;

    /**
     * Cron expression for cron-based scheduling, e.g., "0 0 9 * * *" for 9 AM daily.
     */
    private String cronExpression;

    /**
     * Interval in minutes for interval-based scheduling, e.g., 180 for 3 hours.
     */
    private Integer intervalMinutes;

    /**
     * Start date/time when the rule becomes active.
     */
    private Instant startDate;

    /**
     * End date/time when the rule expires (optional).
     */
    private Instant endDate;

    /**
     * Timezone for scheduling, e.g., "UTC", "America/New_York".
     */
    private String timezone;

    // Status

    /**
     * Whether the rule is currently active.
     */
    @Indexed
    private boolean active;

    /**
     * User ID of the admin who created this rule.
     */
    private String createdBy;

    /**
     * Timestamp when the rule was created.
     */
    @CreatedDate
    private Instant createdAt;

    /**
     * Timestamp when the rule was last modified.
     */
    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Timestamp of the last execution.
     */
    private Instant lastExecutedAt;

    /**
     * Total number of times the rule has been executed.
     */
    private Integer executionCount;

    /**
     * Scheduled timestamp for the next execution.
     */
    @Indexed
    private Instant nextExecutionTime;
}
