package com.alzcare.notification.service;

import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.enums.NotificationStatus;
import com.alzcare.notification.exception.NotificationChannelException;
import com.alzcare.notification.repositories.NotificationRepository;
import com.alzcare.notification.service.channel.ChannelServiceFactory;
import com.alzcare.notification.service.channel.NotificationChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for routing notifications to appropriate channels.
 * <p>Determines the delivery channel using the {@link ChannelServiceFactory}
 * and delegates to the corresponding channel implementation.</p>
 */
@Service
@Slf4j
public class NotificationRoutingService {

    private final ChannelServiceFactory channelFactory;
    private final NotificationRepository notificationRepository;
    private final RetryableNotificationService retryService;

    /**
     * Constructs a new NotificationRoutingService.
     *
     * @param channelFactory the channel service factory
     * @param notificationRepository the notification repository
     * @param retryService the retryable notification service
     */
    public NotificationRoutingService(ChannelServiceFactory channelFactory,
                                      NotificationRepository notificationRepository,
                                      RetryableNotificationService retryService) {
        this.channelFactory = channelFactory;
        this.notificationRepository = notificationRepository;
        this.retryService = retryService;
    }

    /**
     * Routes a notification to its designated channel handler.
     * <p>Gets the appropriate channel from the factory, checks availability,
     * sends the notification, and updates the status accordingly.</p>
     *
     * @param notification the notification to route
     */
    public void routeNotification(Notification notification) {
        try {
            // Get appropriate channel handler
            NotificationChannelService channel = channelFactory.getChannel(notification.getChannel());

            // Check if channel is available
            if (!channel.isAvailable()) {
                throw new NotificationChannelException(
                        "Channel " + notification.getChannel() + " is not available");
            }

            // Send via channel
            channel.send(notification);

            // Update status to SENT (if not already updated by channel)
            if (notification.getStatus() == NotificationStatus.PENDING) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
                notificationRepository.save(notification);
            }

            log.info("Notification routed successfully - id: {}, channel: {}",
                    notification.getId(), notification.getChannel());

        } catch (Exception e) {
            log.error("Failed to send notification via channel: {}", notification.getChannel(), e);
            handleSendFailure(notification, e.getMessage());
        }
    }

    /**
     * Handles notification send failures by triggering retry logic or marking as failed.
     *
     * @param notification the notification that failed to send
     * @param errorMessage the error message from the failure
     */
    private void handleSendFailure(Notification notification, String errorMessage) {
        if (retryService.shouldRetry(notification)) {
            retryService.incrementRetry(notification, errorMessage);
            // The message will be retried by the scheduled job
        } else {
            retryService.incrementRetry(notification, errorMessage);
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
        }
    }
}
