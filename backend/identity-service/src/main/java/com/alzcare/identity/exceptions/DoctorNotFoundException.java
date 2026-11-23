package com.alzcare.identity.exceptions;

import java.util.UUID;

/**
 * Exception thrown when a doctor profile cannot be found.
 * Used for lookups by ID, user ID, or other identifying information.
 */
public class DoctorNotFoundException extends RuntimeException {

    /**
     * Creates a new exception with a message indicating the doctor ID that was not found.
     *
     * @param id the doctor UUID that was not found
     */
    public DoctorNotFoundException(UUID id) {
        super(String.format("Doctor profile not found with id: %s", id));
    }

    /**
     * Creates a new exception with a message indicating the user ID that was not found.
     *
     * @param userId the user ID that was not found
     */
    public DoctorNotFoundException(String userId) {
        super(String.format("Doctor profile not found with userId: %s", userId));
    }

    /**
     * Creates a new exception with a custom message.
     *
     * @param message the detail message
     */
    public DoctorNotFoundException(String message, boolean customMessage) {
        super(message);
    }
}
