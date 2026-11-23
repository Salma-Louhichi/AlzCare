package com.alzcare.notification.service.channel;

/**
 * Enumeration of notification channel types.
 * <p>Defines the different channels through which notifications can be sent.
 * This is the service-layer enum used by channel implementations.</p>
 */
public enum NotificationChannelType {
    SMS,
    EMAIL,
    PUSH,
    IN_APP
}
