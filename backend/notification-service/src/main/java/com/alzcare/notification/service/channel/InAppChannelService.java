package com.alzcare.notification.service.channel;

import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationStatus;
import com.alzcare.notification.exception.NotificationChannelException;
import com.alzcare.notification.repositories.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for handling in-app notifications.
 * <p>Handles in-app notifications by updating the notification status to DELIVERED.
 * The notification is already stored in MongoDB and will be available for the frontend.</p>
 */
@Service
@Slf4j
public class InAppChannelService implements NotificationChannelService {

    private final NotificationRepository notificationRepository;

    /**
     * Constructs a new InAppChannelService.
     *
     * @param notificationRepository the notification repository
     */
    public InAppChannelService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public NotificationChannel getChannelType() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(Notification notification) throws NotificationChannelException {
        // For IN_APP: Just update status to DELIVERED
        // The notification is already stored in MongoDB
        // Frontend will poll or use WebSocket to get notifications

        log.info("[IN_APP] Notification stored for user: {}", notification.getRecipientId());

        // Update status to indicate it's available in-app
        notification.setStatus(NotificationStatus.DELIVERED);
        notification.setDeliveredAt(Instant.now());
        notificationRepository.save(notification);
    }

    @Override
    public boolean isAvailable() {
        return true;  // Always available since we use MongoDB
    }
}
