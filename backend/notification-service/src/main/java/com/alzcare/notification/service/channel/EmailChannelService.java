package com.alzcare.notification.service.channel;

import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.exception.NotificationChannelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for sending email notifications.
 * <p>Current implementation mocks email delivery by logging to console.
 * Structure is ready for SendGrid integration in the future.</p>
 */
@Service
@Slf4j
public class EmailChannelService implements NotificationChannelService {

    /**
     * Constructs a new EmailChannelService.
     */
    public EmailChannelService() {
    }

    @Override
    public NotificationChannel getChannelType() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) throws NotificationChannelException {
        // For now: LOG TO CONSOLE (mock mode)
        // Structure ready for SendGrid integration later

        log.info("[EMAIL MOCK] To: {} | Subject: {} | Body: {}",
                notification.getRecipientId(),
                notification.getTitle(),
                notification.getMessage());

        // TODO: Future - integrate with SendGrid
        // String email = userService.getEmail(notification.getRecipientId());
        // sendGridClient.sendEmail(email, notification.getTitle(), notification.getMessage());
    }

    @Override
    public boolean isAvailable() {
        return true;  // Mock mode
    }
}
