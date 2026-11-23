package com.alzcare.notification.exception;

/**
 * Exception thrown when a notification channel fails to send a notification.
 * <p>Used by channel implementations to signal errors during delivery.</p>
 */
public class NotificationChannelException extends RuntimeException {

    /**
     * Constructs a new NotificationChannelException with the specified message.
     *
     * @param message the detail message
     */
    public NotificationChannelException(String message) {
        super(message);
    }

    /**
     * Constructs a new NotificationChannelException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the underlying cause of the exception
     */
    public NotificationChannelException(String message, Throwable cause) {
        super(message, cause);
    }
}
