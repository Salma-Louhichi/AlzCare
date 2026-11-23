package com.alzcare.notification.client.dto;

import lombok.Data;

/**
 * DTO representing a user from the Identity Service.
 * Used by Feign client for deserializing user data.
 */
@Data
public class IdentityUserResponse {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean enabled;
}