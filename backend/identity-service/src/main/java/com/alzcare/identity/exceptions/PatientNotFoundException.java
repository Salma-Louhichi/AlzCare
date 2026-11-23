package com.alzcare.identity.exceptions;

import java.util.UUID;

/**
 * Exception thrown when a patient profile cannot be found.
 * Used for lookups by ID, Keycloak ID, or other identifying information.
 */
public class PatientNotFoundException extends RuntimeException {

    /**
     * Creates a new exception with a message indicating the patient ID that was not found.
     *
     * @param id the patient UUID that was not found
     */
    public PatientNotFoundException(UUID id) {
        super(String.format("Patient profile not found with id: %s", id));
    }

    /**
     * Creates a new exception with a message indicating the User ID that was not found.
     *
     * @param userId the Keycloak user ID that was not found
     */
    public static PatientNotFoundException byUserId(UUID userId) {
        return new PatientNotFoundException(String.format("Patient profile not found with userId: %s", userId));
    }

    /**
     * Creates a new exception with a custom message.
     *
     * @param message the detail message
     */
    public PatientNotFoundException(String message) {
        super(message);
    }
}
