package com.alzcare.notification.enums;

/**
 * Enumeration of notification statuses.
 * <p>Tracks the lifecycle state of a notification from creation to delivery.</p>
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    READ
}
