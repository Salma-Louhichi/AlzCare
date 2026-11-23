package com.alzcare.identity.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;  // Keycloak user ID
    private String username;
    private String email;
    private UserRole role;
    private UserStatus status;
    private Boolean enabled;
    private Boolean emailVerified;
    private Instant createdAt;
    private Instant lastLogin;
    
    public enum UserRole {
        PATIENT, DOCTOR, CAREGIVER, ADMIN
    }
    
    public enum UserStatus {
        ACTIVE, INACTIVE, PENDING
    }
}
