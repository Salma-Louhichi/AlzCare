package com.alzcare.notification.dto;

import lombok.Data;

/**
 * Data Transfer Object representing a user from the Identity Service.
 * <p>Contains user details including Keycloak UUID, username, email, and account status.</p>
 */
@Data
public class UserDto {

    /**
     * Keycloak UUID - unique identifier for the user.
     */
    private String id;

    /**
     * Username for authentication.
     */
    private String username;

    /**
     * Email address of the user.
     */
    private String email;

    /**
     * First name of the user.
     */
    private String firstName;

    /**
     * Last name of the user.
     */
    private String lastName;

    /**
     * Whether the user account is enabled/active.
     */
    private boolean enabled;
}
