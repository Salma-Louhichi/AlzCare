package com.alzcare.notification.service;

import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.enums.NotificationStatus;
import com.alzcare.notification.repositories.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Service for handling notification retries.
 * <p>Tracks retry attempts and handles the transition to FAILED status
 * when maximum retries are exceeded.</p>
 */
@Component
@Slf4j
public class RetryableNotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Constructs a new RetryableNotificationService.
     *
     * @param notificationRepository the notification repository
     */
    public RetryableNotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Maximum number of retry attempts before marking as FAILED.
     */
    public static final int MAX_RETRIES = 3;

    /**
     * Checks if a notification should be retried based on its current retry count.
     *
     * @param notification the notification to check
     * @return true if the notification should be retried, false if max retries reached
     */
    public boolean shouldRetry(Notification notification) {
        if (notification == null || notification.getRetryCount() == null) {
            return true;
        }
        return notification.getRetryCount() < MAX_RETRIES;
    }

    /**
     * Increments the retry count and updates error information.
     * <p>If maximum retries are reached, sets status to FAILED.</p>
     *
     * @param notification the notification being retried
     * @param errorMessage the error message from the failed attempt
     */
    public void incrementRetry(Notification notification, String errorMessage) {
        if (notification == null) {
            log.warn("Cannot increment retry for null notification");
            return;
        }

        int currentRetry = notification.getRetryCount() != null ? notification.getRetryCount() : 0;
        int newRetryCount = currentRetry + 1;

        notification.setRetryCount(newRetryCount);
        notification.setErrorMessage(errorMessage);

        if (newRetryCount >= MAX_RETRIES) {
            notification.setStatus(NotificationStatus.FAILED);
            log.warn("Notification reached max retries ({}), marking as FAILED - id: {}",
                    MAX_RETRIES, notification.getId());
        } else {
            log.info("Incremented retry count to {} for notification - id: {}",
                    newRetryCount, notification.getId());
        }

        notificationRepository.save(notification);
    }

    /**
     * Gets the remaining retry count for a notification.
     *
     * @param notification the notification to check
     * @return the number of remaining retries
     */
    public int getRemainingRetries(Notification notification) {
        if (notification == null || notification.getRetryCount() == null) {
            return MAX_RETRIES;
        }
        return Math.max(0, MAX_RETRIES - notification.getRetryCount());
    }

    /**
     * Resets the retry count for a notification.
     * <p>Useful when manually reprocessing failed notifications.</p>
     *
     * @param notification the notification to reset
     */
    public void resetRetries(Notification notification) {
        if (notification == null) {
            log.warn("Cannot reset retries for null notification");
            return;
        }

        notification.setRetryCount(0);
        notification.setErrorMessage(null);
        notification.setStatus(NotificationStatus.PENDING);
        notificationRepository.save(notification);

        log.info("Reset retry count for notification - id: {}", notification.getId());
    }

    /**
     * Checks if a notification has exhausted all retry attempts.
     *
     * @param notification the notification to check
     * @return true if max retries have been reached
     */
    public boolean hasExceededMaxRetries(Notification notification) {
        if (notification == null || notification.getRetryCount() == null) {
            return false;
        }
        return notification.getRetryCount() >= MAX_RETRIES;
    }
}
