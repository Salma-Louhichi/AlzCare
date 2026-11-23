package com.alzcare.identity.services;

import com.alzcare.identity.dtos.requests.CaregiverCreateRequest;
import com.alzcare.identity.dtos.requests.DoctorCreateRequest;
import com.alzcare.identity.dtos.requests.PatientCreateRequest;
import com.alzcare.identity.dtos.requests.UserCreateRequest;
import com.alzcare.identity.dtos.requests.UserUpdateRequest;
import com.alzcare.identity.dtos.responses.UserResponse;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    private final Keycloak keycloakAdminClient;
    private final PatientProfileService patientProfileService;
    private final DoctorProfileService doctorProfileService;
    private final CaregiverProfileService caregiverProfileService;

    @Value("${keycloak.realm:alzcare}")
    private String realm;

    /**
     * Get all users from Keycloak
     */
    public List<UserResponse> getAllUsers(String search, int first, int max) {
        var realmResource = keycloakAdminClient.realm(realm);
        
        List<UserRepresentation> users;
        if (search != null && !search.isEmpty()) {
            users = realmResource.users().search(search, first, max);
        } else {
            users = realmResource.users().list(first, max);
        }
        
        return users.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get user by ID
     */
    public Optional<UserResponse> getUserById(String userId) {
        try {
            var userResource = keycloakAdminClient.realm(realm).users().get(userId);
            var user = userResource.toRepresentation();
            return Optional.of(toUserResponse(user));
        } catch (Exception e) {
            log.warn("User not found: {}", userId);
            return Optional.empty();
        }
    }

    /**
     * Create a new user in Keycloak and create corresponding profile based on role.
     * Profile creation is done in a try-catch block to ensure user creation succeeds
     * even if profile creation fails.
     */
    public String createUser(UserCreateRequest request) {
        var realmResource = keycloakAdminClient.realm(realm);
        
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        user.setEmailVerified(request.getEmailVerified() != null ? request.getEmailVerified() : false);
        
        // Set password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));
        
        Response response = realmResource.users().create(user);
        
        if (response.getStatus() != 201) {
            String errorMsg = response.readEntity(String.class);
            log.error("Failed to create user: {}", errorMsg);
            throw new IllegalArgumentException("Failed to create user: " + errorMsg);
        }
        
        // Extract user ID from Location header
        String location = response.getLocation().toString();
        String userId = location.substring(location.lastIndexOf('/') + 1);
        UUID userUuid = UUID.fromString(userId);
        
        // Assign role
        assignRoleToUser(userId, request.getRole().name());
        
        // Create profile based on role
        // If profile creation fails, throw exception to ensure admin knows about the failure
        try {
            createProfileBasedOnRole(userUuid, request);
        } catch (Exception e) {
            log.error("Failed to create profile for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("User created in Keycloak but profile creation failed: " + e.getMessage(), e);
        }
        
        log.info("Created user {} with ID {}", request.getUsername(), userId);
        return userId;
    }

    /**
     * Create profile based on user role.
     * Skips profile creation for ADMIN role.
     */
    private void createProfileBasedOnRole(UUID userId, UserCreateRequest request) {
        switch (request.getRole()) {
            case PATIENT -> createPatientProfile(userId, request);
            case DOCTOR -> createDoctorProfile(userId, request);
            case CAREGIVER -> createCaregiverProfile(userId, request);
            case ADMIN -> {
                // No profile needed for ADMIN
                log.debug("Skipping profile creation for ADMIN user {}", userId);
            }
        }
    }

    /**
     * Create patient profile.
     */
    private void createPatientProfile(UUID userId, UserCreateRequest request) {
        PatientCreateRequest patientRequest = PatientCreateRequest.builder()
                .userId(userId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(parseDate(request.getDateOfBirth()))
                .gender(parseGender(request.getGender()))
                .preferredLanguage(parseLanguage(request.getPreferredLanguage()))
                .photoUrl(request.getPhotoUrl())
                .phone(request.getPhone())
                .emergencyContact(request.getEmergencyContact())
                .address(request.getAddress())
                .build();
        
        patientProfileService.createPatient(patientRequest);
        log.info("Created patient profile for user {}", userId);
    }

    /**
     * Create doctor profile.
     */
    private void createDoctorProfile(UUID userId, UserCreateRequest request) {
        DoctorCreateRequest doctorRequest = DoctorCreateRequest.builder()
                .userId(userId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .photoUrl(request.getPhotoUrlDoctor())
                .speciality(request.getSpeciality())
                .licenseNumber(request.getLicenseNumber())
                .phone(request.getPhoneDoctor())
                .contact(request.getContact())
                .address(request.getAddressDoctor())
                .isAvailable(request.getIsAvailable())
                .build();
        
        doctorProfileService.createDoctor(doctorRequest);
        log.info("Created doctor profile for user {}", userId);
    }

    /**
     * Create caregiver profile.
     */
    private void createCaregiverProfile(UUID userId, UserCreateRequest request) {
        CaregiverCreateRequest caregiverRequest = CaregiverCreateRequest.builder()
                .userId(userId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .photoUrl(request.getPhotoUrlCaregiver())
                .phone(request.getPhoneCaregiver())
                .address(request.getAddressCaregiver())
                .contact(request.getContactCaregiver())
                .isAvailable(request.getIsAvailableCaregiver())
                .isProfessional(request.getIsProfessional())
                .build();
        
        caregiverProfileService.createCaregiverProfile(caregiverRequest);
        log.info("Created caregiver profile for user {}", userId);
    }

    /**
     * Parse date string to LocalDate.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }

    /**
     * Parse gender string to GenderEnum.
     */
    private com.alzcare.identity.enums.GenderEnum parseGender(String genderStr) {
        if (genderStr == null || genderStr.isBlank()) {
            return null;
        }
        try {
            return com.alzcare.identity.enums.GenderEnum.valueOf(genderStr.toUpperCase());
        } catch (Exception e) {
            log.warn("Failed to parse gender: {}", genderStr);
            return null;
        }
    }

    /**
     * Parse language string to LanguageEnum.
     */
    private com.alzcare.identity.enums.LanguageEnum parseLanguage(String langStr) {
        if (langStr == null || langStr.isBlank()) {
            return null;
        }
        try {
            return com.alzcare.identity.enums.LanguageEnum.valueOf(langStr.toUpperCase());
        } catch (Exception e) {
            log.warn("Failed to parse language: {}", langStr);
            return null;
        }
    }

    /**
     * Update user in Keycloak
     */
    public void updateUser(String userId, UserUpdateRequest request) {
        var userResource = keycloakAdminClient.realm(realm).users().get(userId);
        var user = userResource.toRepresentation();
        
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        
        userResource.update(user);
        
        // Update role if changed
        if (request.getRole() != null) {
            updateUserRole(userId, request.getRole().name());
        }
        
        log.info("Updated user {}", userId);
    }

    /**
     * Delete user from Keycloak
     */
    public void deleteUser(String userId) {
        var userResource = keycloakAdminClient.realm(realm).users().get(userId);
        userResource.remove();
        log.info("Deleted user {}", userId);
    }

    /**
     * Reset user password
     */
    public void resetPassword(String userId, String newPassword, boolean temporary) {
        var userResource = keycloakAdminClient.realm(realm).users().get(userId);
        
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(temporary);
        
        userResource.resetPassword(credential);
        log.info("Reset password for user {}", userId);
    }

    // ==================== Helper Methods ====================

    private UserResponse toUserResponse(UserRepresentation user) {
        var realmResource = keycloakAdminClient.realm(realm);
        var roleMappings = realmResource.users().get(user.getId()).roles().getAll();
        
        // Extract primary role
        UserResponse.UserRole role = UserResponse.UserRole.PATIENT;
        if (roleMappings.getRealmMappings() != null) {
            for (RoleRepresentation realmRole : roleMappings.getRealmMappings()) {
                String roleName = realmRole.getName();
                if ("ADMIN".equals(roleName)) {
                    role = UserResponse.UserRole.ADMIN;
                    break;
                } else if ("DOCTOR".equals(roleName)) {
                    role = UserResponse.UserRole.DOCTOR;
                    break;
                } else if ("CAREGIVER".equals(roleName)) {
                    role = UserResponse.UserRole.CAREGIVER;
                    break;
                }
            }
        }
        
        // Determine status
        UserResponse.UserStatus status = user.isEnabled() 
                ? UserResponse.UserStatus.ACTIVE 
                : UserResponse.UserStatus.INACTIVE;
        
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(role)
                .status(status)
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedTimestamp() != null 
                        ? Instant.ofEpochMilli(user.getCreatedTimestamp()) 
                        : null)
                .build();
    }

    private void assignRoleToUser(String userId, String roleName) {
        var realmResource = keycloakAdminClient.realm(realm);
        var role = realmResource.roles().get(roleName).toRepresentation();
        var userResource = realmResource.users().get(userId);
        userResource.roles().realmLevel().add(Collections.singletonList(role));
    }

    private void updateUserRole(String userId, String newRoleName) {
        var realmResource = keycloakAdminClient.realm(realm);
        var userResource = realmResource.users().get(userId);
        
        // Get current realm roles
        var currentRoles = userResource.roles().realmLevel().listAll();
        
        // Remove existing role roles
        var rolesToRemove = currentRoles.stream()
                .filter(r -> List.of("PATIENT", "DOCTOR", "CAREGIVER", "ADMIN").contains(r.getName()))
                .collect(Collectors.toList());
        
        if (!rolesToRemove.isEmpty()) {
            userResource.roles().realmLevel().remove(rolesToRemove);
        }
        
        // Add new role
        var newRole = realmResource.roles().get(newRoleName).toRepresentation();
        userResource.roles().realmLevel().add(Collections.singletonList(newRole));
    }
}
