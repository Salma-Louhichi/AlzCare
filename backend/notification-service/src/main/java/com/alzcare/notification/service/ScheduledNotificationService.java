package com.alzcare.notification.service;

import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationPriority;
import com.alzcare.notification.enums.NotificationStatus;
import com.alzcare.notification.enums.NotificationType;
import com.alzcare.notification.repositories.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for handling scheduled notifications.
 * <p><strong>NOTE:</strong> Replaced by Dynamic Notification Scheduler - rules now configured via admin dashboard.</p>
 * <p>Hardcoded scheduled methods (mood-check, hydration, brain-training) have been removed.
 * Use the NotificationScheduleController (/api/v1/schedules) to create and manage dynamic schedules.</p>
 * <p>Includes a demo mode that creates notifications frequently for demonstration purposes.</p>
 */
@Service
@Slf4j
public class ScheduledNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRoutingService routingService;

    // Actual patient UUIDs from the system (based on frontend logs)
    private static final List<String> DEMO_USER_IDS = List.of(
        "e905f36f-3263-488c-a48a-b87e0df4c3ca",  // Patient from logs
        "0c76bbbc-7a5b-4884-8e13-999387b3994f",  // Another patient from logs
        "demo-user"  // Fallback demo user
    );

    private final AtomicInteger demoUserIndex = new AtomicInteger(0);

    /**
     * Constructs a new ScheduledNotificationService.
     *
     * @param notificationRepository the notification repository
     * @param routingService the notification routing service
     */
    public ScheduledNotificationService(
            NotificationRepository notificationRepository,
            NotificationRoutingService routingService) {
        this.notificationRepository = notificationRepository;
        this.routingService = routingService;
    }

    /*
     * =================================================================================
     * HARD CODED SCHEDULES REMOVED - REPLACED BY DYNAMIC NOTIFICATION SCHEDULER
     * =================================================================================
     * 
     * The following scheduled methods have been removed as they are now replaced
     * by the Dynamic Notification Scheduler:
     * 
     * - sendDailyMoodCheckReminder() - Now configure via admin dashboard
     * - sendHydrationReminders() - Now configure via admin dashboard  
     * - sendBrainTrainingReminder() - Now configure via admin dashboard
     * 
     * Use the NotificationScheduleController endpoints (/api/v1/schedules) to create
     * and manage notification schedules dynamically through the admin dashboard.
     * 
     * The ScheduleBootstrapService loads all active rules from the database on startup.
     * =================================================================================
     */

    /**
     * DEMO MODE: Create a test notification every 30 seconds.
     * This is for demonstration purposes only and only runs in dev profile.
     * Rotates through different notification types for demo variety.
     */
    @Scheduled(fixedRate = 30000)
    @Profile("dev")
    public void createDemoNotification() {
        // Rotate through different notification types for demo variety
        String[] titles = {
                "Daily Check-in 📝",
                "Stay Hydrated 💧",
                "Brain Training Time 🧠",
                "Medication Reminder 💊",
                "Caregiver Check-in 👋"
        };
        String[] messages = {
                "How are you feeling today? Log your thoughts!",
                "Have you had water in the last hour?",
                "Time for your daily cognitive exercise!",
                "Don't forget to take your prescribed medication.",
                "Your caregiver wants to know how you're doing!"
        };
        NotificationType[] types = {
                NotificationType.REMINDER,
                NotificationType.REMINDER,
                NotificationType.REMINDER,
                NotificationType.ALERT,
                NotificationType.SYSTEM
        };

        // Rotate based on current time (every 30 seconds)
        int index = (int) (System.currentTimeMillis() / 30000) % titles.length;
        
        // Rotate through users to distribute notifications
        String userId = DEMO_USER_IDS.get(demoUserIndex.getAndIncrement() % DEMO_USER_IDS.size());

        Notification notification = Notification.builder()
                .recipientId(userId)
                .type(types[index])
                .channel(NotificationChannel.IN_APP)
                .priority(index == 3 ? NotificationPriority.HIGH : NotificationPriority.NORMAL)
                .title(titles[index])
                .message(messages[index])
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .metadata(Map.of(
                        "demo", true,
                        "category", "demo"
                ))
                .build();

        Notification saved = notificationRepository.save(notification);
        routingService.routeNotification(saved);
        log.info("[DEMO] Created notification for user {}: {} - {}", userId, saved.getId(), titles[index]);
    }
}
