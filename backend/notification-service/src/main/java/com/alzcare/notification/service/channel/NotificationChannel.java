package com.alzcare.notification.service.channel;

import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.exception.NotificationChannelException;

/**
 * Interface for notification channel implementations.
 * <p>Defines the contract for sending notifications through various channels
 * such as SMS, Email, Push, and In-App notifications.</p>
 */
public interface NotificationChannel {

    /**
     * Gets the channel type this implementation handles.
     *
     * @return the {@link NotificationChannelType} for this channel
     */
    NotificationChannelType getChannelType();

    /**
     * Sends a notification through this channel.
     *
     * @param notification the notification to send
     * @throws NotificationChannelException if sending fails
     */
    void send(Notification notification) throws NotificationChannelException;

    /**
     * Checks if the channel is configured and available for sending.
     *
     * @return true if the channel is available, false otherwise
     */
    boolean isAvailable();
}
