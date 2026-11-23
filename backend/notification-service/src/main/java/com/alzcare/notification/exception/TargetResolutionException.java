package com.alzcare.notification.exception;

/**
 * Runtime exception thrown when target user resolution fails.
 * <p>This exception is used to indicate errors during the process of
 * resolving target users for scheduled notifications, such as failures
 * when calling the Identity Service.</p>
 */
public class TargetResolutionException extends RuntimeException {

    /**
     * Constructs a new target resolution exception with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public TargetResolutionException(String message) {
        super(message);
    }

    /**
     * Constructs a new target resolution exception with the specified detail message
     * and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause   the underlying cause of the exception
     */
    public TargetResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
