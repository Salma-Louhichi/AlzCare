package com.alzcare.notification.exception;

/**
 * Exception thrown when a requested resource is not found.
 * <p>Used by controllers to signal 404 Not Found responses.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new ResourceNotFoundException with the specified resource type and ID.
     *
     * @param resource the type of resource that was not found (e.g., "Notification", "Template")
     * @param id the identifier of the resource that was not found
     */
    public ResourceNotFoundException(String resource, String id) {
        super(String.format("%s not found with id: %s", resource, id));
    }
}
