package com.alzcare.notification.config;

import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationPriority;
import com.alzcare.notification.enums.NotificationStatus;
import com.alzcare.notification.enums.NotificationType;
import com.alzcare.notification.repositories.NotificationRepository;
import com.alzcare.notification.service.NotificationRoutingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Demo data initializer for development environment.
 * <p>Creates sample notifications on application startup for demonstration purposes.</p>
 * <p>This initializer runs only in the 'dev' profile to avoid affecting production.</p>
 */
@Component
@Profile("dev")
@Slf4j
public class DemoDataInitializer implements CommandLineRunner {

    private final NotificationRepository notificationRepository;
    private final NotificationRoutingService routingService;

    // Actual patient UUIDs from the system (based on frontend logs)
    private static final List<String> DEMO_USER_IDS = List.of(
        "e905f36f-3263-488c-a48a-b87e0df4c3ca",  // Patient from logs
        "0c76bbbc-7a5b-4884-8e13-999387b3994f",  // Another patient from logs
        "demo-user"  // Fallback demo user
    );

    public DemoDataInitializer(NotificationRepository notificationRepository,
                               NotificationRoutingService routingService) {
        this.notificationRepository = notificationRepository;
        this.routingService = routingService;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing demo notifications for dev environment...");

        for (String userId : DEMO_USER_IDS) {
            createDemoNotificationsForUser(userId);
        }

        log.info("Demo notifications initialized successfully for {} users", DEMO_USER_IDS.size());
    }

    private void createDemoNotificationsForUser(String userId) {
        // Create a welcome notification
        createNotification(userId, 
            "Welcome to AlzCare! 👋",
            "Your notification system is working. You'll receive reminders here.",
            NotificationType.SYSTEM);

        // Create a few initial demo notifications with different types
        createNotification(userId,
            "Daily Check-in 📝",
            "How are you feeling today? Take a moment to log your thoughts in the memory journal.",
            NotificationType.REMINDER);

        createNotification(userId,
            "Stay Hydrated 💧",
            "Have you had water in the last hour? Stay hydrated to keep your mind sharp!",
            NotificationType.REMINDER);

        createNotification(userId,
            "Medication Reminder 💊",
            "Time to take your afternoon medication. Don't forget to mark it as taken!",
            NotificationType.REMINDER);

        log.info("Created demo notifications for user: {}", userId);
    }

    private void createNotification(String userId, String title, String message, NotificationType type) {
        Notification notification = Notification.builder()
                .recipientId(userId)
                .type(type)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.NORMAL)
                .title(title)
                .message(message)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();

        Notification saved = notificationRepository.save(notification);
        routingService.routeNotification(saved);
    }
}
