package com.alzcare.notification.service.channel;

import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.exception.NotificationChannelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for sending SMS notifications.
 * <p>Current implementation mocks SMS delivery by logging to console.
 * Structure is ready for Twilio integration in the future.</p>
 */
@Service
@Slf4j
public class SmsChannelService implements NotificationChannelService {

    /**
     * Constructs a new SmsChannelService.
     */
    public SmsChannelService() {
    }

    @Override
    public NotificationChannel getChannelType() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(Notification notification) throws NotificationChannelException {
        // For now: LOG TO CONSOLE (mock mode)
        // Structure ready for Twilio integration later

        log.info("[SMS MOCK] To: {} | Message: {}",
                notification.getRecipientId(),
                notification.getMessage());

        // TODO: Future - integrate with Twilio
        // String phoneNumber = userService.getPhoneNumber(notification.getRecipientId());
        // twilioClient.sendSms(phoneNumber, notification.getMessage());
    }

    @Override
    public boolean isAvailable() {
        // For now: always true (mock)
        // Future: check Twilio credentials configured
        return true;
    }
}
