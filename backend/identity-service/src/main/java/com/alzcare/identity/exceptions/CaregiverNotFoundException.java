package com.alzcare.identity.exceptions;

import java.util.UUID;

/**
 * Exception thrown when a caregiver profile cannot be found.
 * Used for lookups by ID, user ID, or other identifying information.
 */
public class CaregiverNotFoundException extends RuntimeException {

    /**
     * Creates a new exception with a message indicating the caregiver ID that was not found.
     *
     * @param id the caregiver UUID that was not found
     */
    public CaregiverNotFoundException(UUID id) {
        super(String.format("Caregiver profile not found with id: %s", id));
    }

    /**
     * Creates a new exception with a custom message.
     *
     * @param message the detail message
     */
    public CaregiverNotFoundException(String message) {
        super(message);
    }
}
