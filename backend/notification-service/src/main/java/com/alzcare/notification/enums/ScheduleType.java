package com.alzcare.notification.enums;

/**
 * Enumeration of schedule types for notification rules.
 * <p>Defines the scheduling mechanism used to determine when notifications
 * should be sent - either at fixed intervals, using cron expressions, or one-time.</p>
 */
public enum ScheduleType {
    INTERVAL,
    CRON,
    ONE_TIME
}
