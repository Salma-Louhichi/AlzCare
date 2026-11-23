package com.alzcare.notification.config;

import com.alzcare.notification.entities.ScheduledNotificationRule;
import com.alzcare.notification.repositories.NotificationScheduleRepository;
import com.alzcare.notification.service.dynamic.DynamicSchedulerService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Bootstrap service that loads and schedules all active notification rules
 * on application startup. Ensures persisted schedules are restored after
 * application restart.
 * <p>Handles validation of expired rules and gracefully handles scheduling errors.</p>
 */
@Component
@Slf4j
public class ScheduleBootstrapService {

    private final NotificationScheduleRepository scheduleRepository;
    private final DynamicSchedulerService dynamicScheduler;
    private final boolean bootstrapEnabled;

    /**
     * Constructs a new ScheduleBootstrapService.
     *
     * @param scheduleRepository the repository for scheduled notification rules
     * @param dynamicScheduler   the dynamic scheduler service for registering rules
     * @param bootstrapEnabled   flag to enable/disable bootstrap on startup
     */
    public ScheduleBootstrapService(
            NotificationScheduleRepository scheduleRepository,
            DynamicSchedulerService dynamicScheduler,
            @Value("${notification.scheduler.bootstrap-on-startup:true}") boolean bootstrapEnabled) {
        this.scheduleRepository = scheduleRepository;
        this.dynamicScheduler = dynamicScheduler;
        this.bootstrapEnabled = bootstrapEnabled;
    }

    /**
     * Initializes the scheduler by loading all active rules from the database
     * and registering them with the dynamic scheduler.
     * <p>This method runs after the bean is constructed and dependencies are injected.
     * It performs the following steps:
     * <ol>
     *   <li>Checks if bootstrap is enabled via configuration</li>
     *   <li>Loads all active rules from the database</li>
     *   <li>Validates each rule (checks expiration)</li>
     *   <li>Schedules valid rules with the dynamic scheduler</li>
     *   <li>Handles errors gracefully per-rule</li>
     * </ol>
     */
    @PostConstruct
    public void initialize() {
        if (!bootstrapEnabled) {
            log.info("Schedule bootstrap is disabled via configuration. Skipping schedule initialization.");
            return;
        }

        log.info("Starting schedule bootstrap - loading active notification rules...");

        try {
            // Cancel any existing scheduled tasks (clean slate)
            dynamicScheduler.cancelAllSchedules();
            
            // Load all active schedules from DB
            List<ScheduledNotificationRule> activeRules = scheduleRepository.findByActiveTrue();
            log.info("Found {} active scheduled notification rules", activeRules.size());

            int scheduledCount = 0;
            int expiredCount = 0;
            int failedCount = 0;

            // Validate and schedule each rule
            for (ScheduledNotificationRule rule : activeRules) {
                try {
                    // Check if end date hasn't passed
                    if (rule.getEndDate() != null && Instant.now().isAfter(rule.getEndDate())) {
                        log.warn("Rule {} has expired (end date: {}), deactivating", 
                                rule.getId(), rule.getEndDate());
                        rule.setActive(false);
                        scheduleRepository.save(rule);
                        expiredCount++;
                        continue;
                    }

                    dynamicScheduler.scheduleRule(rule);
                    log.info("Scheduled rule: {} - {}", rule.getId(), rule.getName());
                    scheduledCount++;
                } catch (Exception e) {
                    log.error("Failed to schedule rule {}: {}", rule.getId(), e.getMessage(), e);
                    failedCount++;
                }
            }

            log.info("Schedule bootstrap completed. Scheduled: {}, Expired: {}, Failed: {}", 
                    scheduledCount, expiredCount, failedCount);

        } catch (Exception e) {
            log.error("Failed to bootstrap schedules: {}", e.getMessage(), e);
        }
    }
}
