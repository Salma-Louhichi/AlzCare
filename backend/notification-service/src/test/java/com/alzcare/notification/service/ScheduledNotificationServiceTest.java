package com.alzcare.notification.service;

import org.junit.jupiter.api.Disabled;
import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.repositories.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the scheduled notification service.
 * <p>Verifies that scheduled tasks correctly create notifications.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Disabled("Needs a running MongoDB on localhost and waits for real @Scheduled ticks (30 s+). "
        + "Convert to Testcontainers with a controllable clock - see IMPROVEMENTS.md BLD-2.")
class ScheduledNotificationServiceTest {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Tests that scheduled tasks create notifications.
     * <p>This test waits a few seconds for the @Scheduled tasks to run,
     * then verifies that notifications were created in the database.</p>
     *
     * @throws InterruptedException if the sleep is interrupted
     */
    @Test
    void scheduledTasksCreateNotifications() throws InterruptedException {
        // Clear existing notifications first
        notificationRepository.deleteAll();

        // Wait a bit for @Scheduled to run (demo notification runs every 30 seconds)
        // We wait 35 seconds to ensure at least one demo notification is created
        Thread.sleep(35000);

        // Verify notifications were created
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).isNotEmpty();
        assertThat(notifications).anyMatch(n -> n.getRecipientId().equals("demo-user"));
    }

    /**
     * Tests that demo notifications have the expected structure.
     */
    @Test
    void demoNotificationsHaveCorrectStructure() throws InterruptedException {
        // Clear existing notifications first
        notificationRepository.deleteAll();

        // Wait for demo notification to be created
        Thread.sleep(35000);

        // Find notifications for demo-user
        List<Notification> notifications = notificationRepository.findAll();

        assertThat(notifications).isNotEmpty();

        Notification demoNotification = notifications.stream()
                .filter(n -> "demo-user".equals(n.getRecipientId()))
                .findFirst()
                .orElse(null);

        assertThat(demoNotification).isNotNull();
        assertThat(demoNotification.getTitle()).isNotBlank();
        assertThat(demoNotification.getMessage()).isNotBlank();
        assertThat(demoNotification.getChannel()).isNotNull();
        assertThat(demoNotification.getType()).isNotNull();
    }
}
