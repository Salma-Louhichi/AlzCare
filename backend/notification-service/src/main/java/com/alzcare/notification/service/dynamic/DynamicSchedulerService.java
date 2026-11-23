package com.alzcare.notification.service.dynamic;

import com.alzcare.notification.entities.ScheduledNotificationRule;
import com.alzcare.notification.enums.ScheduleType;
import com.alzcare.notification.service.execution.ScheduleExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Dynamic Scheduler Service for managing scheduled notification rules.
 * <p>Provides thread-safe operations for scheduling, canceling, and rescheduling
 * notification rules using Spring's TaskScheduler. Supports interval-based,
 * cron-based, and one-time scheduling mechanisms.</p>
 */
@Service
@Slf4j
public class DynamicSchedulerService {

    private final TaskScheduler taskScheduler;
    private final ScheduleExecutionService executionService;

    /**
     * Thread-safe map to track active scheduled tasks by rule ID.
     */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * Constructs a new DynamicSchedulerService.
     *
     * @param taskScheduler    the Spring TaskScheduler for scheduling tasks
     * @param executionService the service responsible for executing scheduled notifications
     */
    public DynamicSchedulerService(TaskScheduler taskScheduler, ScheduleExecutionService executionService) {
        this.taskScheduler = taskScheduler;
        this.executionService = executionService;
    }

    /**
     * Schedules a notification rule. Creates and registers a scheduled task
     * based on the rule's schedule type (INTERVAL, CRON, or ONE_TIME).
     *
     * @param rule the scheduled notification rule to schedule
     * @return the ScheduledFuture representing the scheduled task
     * @throws IllegalArgumentException if the rule is null, inactive, or has an unsupported schedule type
     */
    public ScheduledFuture<?> scheduleRule(ScheduledNotificationRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Rule cannot be null");
        }

        if (!rule.isActive()) {
            log.warn("Cannot schedule inactive rule: {}", rule.getId());
            throw new IllegalArgumentException("Cannot schedule inactive rule: " + rule.getId());
        }

        // Cancel any existing schedule for this rule
        cancelSchedule(rule.getId());

        ScheduleType scheduleType = rule.getScheduleType();
        if (scheduleType == null) {
            throw new IllegalArgumentException("Schedule type cannot be null for rule: " + rule.getId());
        }

        ScheduledFuture<?> future;
        Runnable task = createTask(rule);

        switch (scheduleType) {
            case INTERVAL:
                future = scheduleIntervalTask(rule, task);
                break;
            case CRON:
                future = scheduleCronTask(rule, task);
                break;
            case ONE_TIME:
                future = scheduleOneTimeTask(rule, task);
                break;
            default:
                throw new IllegalArgumentException("Unsupported schedule type: " + scheduleType);
        }

        scheduledTasks.put(rule.getId(), future);
        log.info("Scheduled rule [{}] with type [{}]", rule.getId(), scheduleType);

        return future;
    }

    /**
     * Cancels a scheduled task by rule ID.
     *
     * @param ruleId the ID of the rule to cancel
     * @return true if the task was found and canceled, false otherwise
     */
    public boolean cancelSchedule(String ruleId) {
        if (ruleId == null) {
            return false;
        }

        ScheduledFuture<?> future = scheduledTasks.remove(ruleId);
        if (future != null) {
            boolean canceled = future.cancel(false);
            if (canceled) {
                log.info("Cancelled scheduled task for rule: {}", ruleId);
            } else {
                log.warn("Failed to cancel scheduled task for rule: {}", ruleId);
            }
            return canceled;
        }

        log.debug("No active schedule found for rule: {}", ruleId);
        return false;
    }

    /**
     * Updates/reschedules an existing rule by canceling the old schedule
     * and creating a new one with the updated rule configuration.
     *
     * @param rule the updated scheduled notification rule
     * @return the new ScheduledFuture representing the rescheduled task
     * @throws IllegalArgumentException if the rule is null or inactive
     */
    public ScheduledFuture<?> rescheduleRule(ScheduledNotificationRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Rule cannot be null");
        }

        log.info("Rescheduling rule: {}", rule.getId());

        // Cancel existing schedule if present
        cancelSchedule(rule.getId());

        // Schedule with new configuration
        return scheduleRule(rule);
    }

    /**
     * Cancels all active scheduled tasks.
     * <p>This operation is thread-safe and will attempt to cancel all
     * tasks currently tracked in the scheduled tasks map.</p>
     */
    public void cancelAllSchedules() {
        log.info("Cancelling all {} active scheduled tasks", scheduledTasks.size());

        scheduledTasks.forEach((ruleId, future) -> {
            if (future != null && !future.isDone()) {
                future.cancel(false);
                log.debug("Cancelled scheduled task for rule: {}", ruleId);
            }
        });

        scheduledTasks.clear();
        log.info("All scheduled tasks have been cancelled");
    }

    /**
     * Gets all currently active scheduled task IDs.
     *
     * @return a set of rule IDs that have active scheduled tasks
     */
    public Set<String> getActiveScheduleIds() {
        // Clean up completed tasks before returning
        scheduledTasks.entrySet().removeIf(entry -> {
            ScheduledFuture<?> future = entry.getValue();
            return future == null || future.isDone();
        });

        return Set.copyOf(scheduledTasks.keySet());
    }

    /**
     * Checks if a rule is currently scheduled and active.
     *
     * @param ruleId the ID of the rule to check
     * @return true if the rule has an active scheduled task, false otherwise
     */
    public boolean isScheduled(String ruleId) {
        if (ruleId == null) {
            return false;
        }

        ScheduledFuture<?> future = scheduledTasks.get(ruleId);
        return future != null && !future.isDone();
    }

    /**
     * Schedules an interval-based task using scheduleAtFixedRate.
     *
     * @param rule the rule containing interval configuration
     * @param task the runnable task to execute
     * @return the ScheduledFuture representing the scheduled task
     * @throws IllegalArgumentException if intervalMinutes is null or invalid
     */
    private ScheduledFuture<?> scheduleIntervalTask(ScheduledNotificationRule rule, Runnable task) {
        if (rule.getIntervalMinutes() == null || rule.getIntervalMinutes() <= 0) {
            throw new IllegalArgumentException("Invalid interval minutes for rule: " + rule.getId());
        }

        long intervalMs = rule.getIntervalMinutes() * 60 * 1000L;
        long initialDelayMs = calculateInitialDelay(rule.getStartDate());

        Date startTime = new Date(System.currentTimeMillis() + initialDelayMs);

        log.debug("Scheduling interval task for rule [{}] with interval [{}]ms, initial delay [{}]ms",
                rule.getId(), intervalMs, initialDelayMs);

        return taskScheduler.scheduleAtFixedRate(task, startTime, intervalMs);
    }

    /**
     * Schedules a cron-based task using CronTrigger.
     *
     * @param rule the rule containing cron expression
     * @param task the runnable task to execute
     * @return the ScheduledFuture representing the scheduled task
     * @throws IllegalArgumentException if cronExpression is null or empty
     */
    private ScheduledFuture<?> scheduleCronTask(ScheduledNotificationRule rule, Runnable task) {
        if (rule.getCronExpression() == null || rule.getCronExpression().trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression cannot be null or empty for rule: " + rule.getId());
        }

        String timezone = rule.getTimezone() != null ? rule.getTimezone() : "UTC";
        CronTrigger cronTrigger = new CronTrigger(rule.getCronExpression(), ZoneId.of(timezone));

        log.debug("Scheduling cron task for rule [{}] with expression [{}] in timezone [{}]",
                rule.getId(), rule.getCronExpression(), timezone);

        return taskScheduler.schedule(task, cronTrigger);
    }

    /**
     * Schedules a one-time task at the specified start date.
     *
     * @param rule the rule containing start date
     * @param task the runnable task to execute
     * @return the ScheduledFuture representing the scheduled task
     */
    private ScheduledFuture<?> scheduleOneTimeTask(ScheduledNotificationRule rule, Runnable task) {
        long initialDelayMs = calculateInitialDelay(rule.getStartDate());
        Instant executionTime = Instant.now().plusMillis(initialDelayMs);

        log.debug("Scheduling one-time task for rule [{}] at [{}]",
                rule.getId(), executionTime);

        return taskScheduler.schedule(task, executionTime);
    }

    /**
     * Creates a runnable task wrapper that checks end date before execution.
     *
     * @param rule the scheduled notification rule
     * @return a runnable that wraps the execution logic
     */
    private Runnable createTask(ScheduledNotificationRule rule) {
        return () -> {
            try {
                // Check if the rule has expired
                if (rule.getEndDate() != null && Instant.now().isAfter(rule.getEndDate())) {
                    log.info("Rule [{}] has passed its end date, cancelling schedule", rule.getId());
                    cancelSchedule(rule.getId());
                    return;
                }

                // Execute via execution service
                executionService.executeSchedule(rule.getId());

            } catch (Exception e) {
                log.error("Error executing scheduled task for rule [{}]: {}", rule.getId(), e.getMessage(), e);
            }
        };
    }

    /**
     * Calculates the initial delay in milliseconds based on the start date.
     *
     * @param startDate the desired start date/time, may be null
     * @return the delay in milliseconds (0 if startDate is null or in the past)
     */
    private long calculateInitialDelay(Instant startDate) {
        if (startDate == null) {
            return 0;
        }

        Instant now = Instant.now();
        if (startDate.isAfter(now)) {
            return startDate.toEpochMilli() - now.toEpochMilli();
        }

        // Start date is in the past, schedule immediately
        return 0;
    }

    /**
     * Gets the count of currently active scheduled tasks.
     *
     * @return the number of active scheduled tasks
     */
    public int getActiveTaskCount() {
        return scheduledTasks.size();
    }
}
