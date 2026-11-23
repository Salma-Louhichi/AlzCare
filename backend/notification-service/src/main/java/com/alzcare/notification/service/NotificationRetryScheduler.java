package com.alzcare.notification.service;

import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.enums.NotificationStatus;
import com.alzcare.notification.repositories.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled task for retrying failed notifications.
 * <p>Periodically checks for notifications with FAILED status and retry count
 * below the maximum threshold, then attempts to re-route them.</p>
 */
@Component
@Slf4j
@EnableScheduling
public class NotificationRetryScheduler {

    private final NotificationRepository notificationRepository;
    private final NotificationRoutingService routingService;

    public NotificationRetryScheduler(NotificationRepository notificationRepository,
                                      NotificationRoutingService routingService) {
        this.notificationRepository = notificationRepository;
        this.routingService = routingService;
    }

    /**
     * Retries failed notifications every minute.
     * <p>Finds notifications with status FAILED and retry count less than 3,
     * resets their status to PENDING, and re-routes them for delivery.</p>
     */
    @Scheduled(fixedDelay = 60000)  // Every minute
    public void retryFailedNotifications() {
        // Find notifications with status FAILED and retryCount < MAX_RETRIES
        List<Notification> retryable = notificationRepository
                .findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3);

        if (retryable.isEmpty()) {
            return;
        }

        log.info("Found {} notifications to retry", retryable.size());

        for (Notification notification : retryable) {
            log.info("Retrying notification: {}", notification.getId());
            notification.setStatus(NotificationStatus.PENDING);
            notificationRepository.save(notification);
            routingService.routeNotification(notification);
        }
    }
}
