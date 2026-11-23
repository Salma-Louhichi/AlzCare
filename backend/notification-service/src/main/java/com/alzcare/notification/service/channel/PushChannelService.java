package com.alzcare.notification.service.channel;

import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.exception.NotificationChannelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for sending push notifications.
 * <p>Current implementation mocks push delivery by logging to console.
 * Structure is ready for Firebase Cloud Messaging integration in the future.</p>
 */
@Service
@Slf4j
public class PushChannelService implements NotificationChannelService {

    /**
     * Constructs a new PushChannelService.
     */
    public PushChannelService() {
    }

    @Override
    public NotificationChannel getChannelType() {
        return NotificationChannel.PUSH;
    }

    @Override
    public void send(Notification notification) throws NotificationChannelException {
        // For now: LOG TO CONSOLE (mock mode)
        // Structure ready for Firebase integration later

        log.info("[PUSH MOCK] To: {} | Title: {} | Body: {}",
                notification.getRecipientId(),
                notification.getTitle(),
                notification.getMessage());

        // TODO: Future - integrate with Firebase Cloud Messaging
        // String fcmToken = userService.getFcmToken(notification.getRecipientId());
        // firebaseClient.sendPush(fcmToken, notification.getTitle(), notification.getMessage());
    }

    @Override
    public boolean isAvailable() {
        return true;  // Mock mode
    }
}
