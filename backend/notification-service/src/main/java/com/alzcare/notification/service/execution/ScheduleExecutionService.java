package com.alzcare.notification.service.execution;

import com.alzcare.notification.dto.UserDto;
import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.entities.ScheduledNotificationRule;
import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationStatus;
import com.alzcare.notification.enums.ScheduleType;
import com.alzcare.notification.exception.ResourceNotFoundException;
import com.alzcare.notification.repositories.NotificationRepository;
import com.alzcare.notification.repositories.NotificationScheduleRepository;
import com.alzcare.notification.service.NotificationRoutingService;
import com.alzcare.notification.service.targeting.IdentityServiceClientFeign;
import com.alzcare.notification.service.targeting.TargetUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for executing scheduled notification rules.
 * <p>Loads notification rules, resolves target users, processes message templates
 * with user-specific variables, creates notification records, and routes them to
 * appropriate delivery channels. Also updates execution metrics and calculates
 * the next execution time for recurring schedules.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleExecutionService {

    private final NotificationScheduleRepository repository;
    private final TargetUserResolver targetUserResolver;
    private final NotificationRepository notificationRepository;
    private final NotificationRoutingService routingService;
    private final IdentityServiceClientFeign identityServiceClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Executes a scheduled notification rule.
     * Loads the rule, resolves target users, creates notifications.
     *
     * @param ruleId the ID of the scheduled rule to execute
     */
    @Transactional
    public void executeSchedule(String ruleId) {
        log.info("Executing scheduled notification rule: {}", ruleId);

        // Load Rule
        ScheduledNotificationRule rule = repository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledNotificationRule", ruleId));

        // Check if Active
        if (!rule.isActive()) {
            log.warn("Skipping inactive rule: {}", ruleId);
            return;
        }

        // Check if end date has passed
        if (rule.getEndDate() != null && Instant.now().isAfter(rule.getEndDate())) {
            log.warn("Rule {} has expired, deactivating", ruleId);
            rule.setActive(false);
            repository.save(rule);
            return;
        }

        // Resolve Target Users
        List<String> targetUserIds = targetUserResolver.resolveTargetUsers(rule);
        if (targetUserIds.isEmpty()) {
            log.warn("No target users found for rule: {}", ruleId);
            return;
        }

        log.info("Creating notifications for {} target users for rule: {}", targetUserIds.size(), ruleId);

        // Create Notifications for each target user
        NotificationChannel primaryChannel = rule.getChannels().get(0);
        int successCount = 0;

        for (String userId : targetUserIds) {
            try {
                // Fetch user context for template processing
                UserContext context = buildUserContext(userId);

                Notification notification = Notification.builder()
                        .recipientId(userId)
                        .type(rule.getType())
                        .channel(primaryChannel)
                        .priority(rule.getPriority())
                        .title(processTemplate(rule.getTitleTemplate(), context))
                        .message(processTemplate(rule.getMessageTemplate(), context))
                        .status(NotificationStatus.PENDING)
                        .metadata(Map.of("scheduleId", ruleId))
                        .build();

                notificationRepository.save(notification);
                routingService.routeNotification(notification);
                successCount++;

            } catch (Exception e) {
                log.error("Failed to create notification for user {} in rule {}", userId, ruleId, e);
            }
        }

        log.info("Successfully created and routed {} notifications for rule: {}", successCount, ruleId);

        // Update Execution Metrics
        updateExecutionMetrics(rule);

        log.info("Completed execution of rule: {}", ruleId);
    }

    /**
     * Processes a template string by replacing variables with actual values.
     * <p>Supported variables:
     * <ul>
     *   <li>{{userName}} - user's username</li>
     *   <li>{{firstName}} - user's first name</li>
     *   <li>{{currentDate}} - current date (yyyy-MM-dd)</li>
     *   <li>{{currentTime}} - current time (HH:mm)</li>
     * </ul>
     * </p>
     *
     * @param template the template string containing variables
     * @param context the user context containing values for variables
     * @return the processed template with variables replaced
     */
    private String processTemplate(String template, UserContext context) {
        if (template == null) {
            return null;
        }

        String result = template;

        // Replace user-specific variables
        if (context != null) {
            result = result.replace("{{userName}}", context.getUserName() != null ? context.getUserName() : "User");
            result = result.replace("{{firstName}}", context.getFirstName() != null ? context.getFirstName() : "");
        } else {
            result = result.replace("{{userName}}", "User");
            result = result.replace("{{firstName}}", "");
        }

        // Replace date/time variables
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate currentDate = LocalDate.now(zoneId);
        LocalTime currentTime = LocalTime.now(zoneId);

        result = result.replace("{{currentDate}}", currentDate.format(DATE_FORMATTER));
        result = result.replace("{{currentTime}}", currentTime.format(TIME_FORMATTER));

        return result;
    }

    /**
     * Builds a user context for template processing by fetching user details.
     *
     * @param userId the user ID to fetch context for
     * @return the user context containing user details
     */
    private UserContext buildUserContext(String userId) {
        try {
            UserDto user = identityServiceClient.getUserById(userId);
            if (user != null) {
                return UserContext.builder()
                        .userId(user.getId())
                        .userName(user.getUsername())
                        .firstName(user.getFirstName())
                        .email(user.getEmail())
                        .build();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user context for userId: {}", userId, e);
        }
        return UserContext.builder()
                .userId(userId)
                .userName("User")
                .build();
    }

    /**
     * Updates execution metrics for the rule including last execution time,
     * execution count, and calculates the next execution time.
     *
     * @param rule the scheduled notification rule to update
     */
    private void updateExecutionMetrics(ScheduledNotificationRule rule) {
        rule.setLastExecutedAt(Instant.now());
        rule.setExecutionCount(rule.getExecutionCount() != null ? rule.getExecutionCount() + 1 : 1);
        rule.setNextExecutionTime(calculateNextExecution(rule));

        // For one-time schedules, deactivate after execution
        if (rule.getScheduleType() == ScheduleType.ONE_TIME) {
            rule.setActive(false);
            log.info("Deactivating one-time rule after execution: {}", rule.getId());
        }

        repository.save(rule);
        log.debug("Updated execution metrics for rule: {}", rule.getId());
    }

    /**
     * Calculates next execution time based on schedule type.
     *
     * @param rule the scheduled notification rule
     * @return the calculated next execution time, or null if no more executions
     */
    private Instant calculateNextExecution(ScheduledNotificationRule rule) {
        if (rule.getScheduleType() == null) {
            return null;
        }

        Instant now = Instant.now();
        ZoneId zoneId = rule.getTimezone() != null ? ZoneId.of(rule.getTimezone()) : ZoneId.systemDefault();

        switch (rule.getScheduleType()) {
            case INTERVAL:
                if (rule.getIntervalMinutes() != null && rule.getIntervalMinutes() > 0) {
                    return now.plusSeconds(rule.getIntervalMinutes() * 60L);
                }
                log.warn("Invalid interval for rule: {}", rule.getId());
                return null;

            case CRON:
                if (rule.getCronExpression() != null && !rule.getCronExpression().isEmpty()) {
                    try {
                        CronExpression cronExpression = CronExpression.parse(rule.getCronExpression());
                        return cronExpression.next(now);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid cron expression for rule {}: {}", rule.getId(), rule.getCronExpression(), e);
                        return null;
                    }
                }
                log.warn("Missing cron expression for rule: {}", rule.getId());
                return null;

            case ONE_TIME:
                // One-time schedules don't have a next execution
                return null;

            default:
                log.warn("Unknown schedule type for rule {}: {}", rule.getId(), rule.getScheduleType());
                return null;
        }
    }

    /**
     * Inner class representing the context of a user for template processing.
     */
    @lombok.Builder
    @lombok.Data
    private static class UserContext {
        private String userId;
        private String userName;
        private String firstName;
        private String email;
    }
}
