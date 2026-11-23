package com.alzcare.identity.controllers;

import com.alzcare.identity.dtos.requests.UserCreateRequest;
import com.alzcare.identity.dtos.requests.UserFilter;
import com.alzcare.identity.dtos.requests.UserUpdateRequest;
import com.alzcare.identity.dtos.responses.PaginatedResponse;
import com.alzcare.identity.dtos.responses.UserResponse;
import com.alzcare.identity.services.KeycloakAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user management operations.
 * Provides endpoints for admin users to manage all users.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Admin", description = "User management APIs for administrators")
public class UserAdminController {

    private final KeycloakAdminService keycloakAdminService;

    /**
     * Get all users with optional filtering and pagination
     */
    @GetMapping
    @Operation(summary = "List all users", description = "Get paginated list of all users with optional filtering")
    public ResponseEntity<PaginatedResponse<UserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // Get users from Keycloak
        List<UserResponse> users = keycloakAdminService.getAllUsers(search, page * size, size);
        
        // Apply role filter if specified
        if (role != null && !role.isEmpty()) {
            users = users.stream()
                    .filter(u -> u.getRole().name().equalsIgnoreCase(role))
                    .toList();
        }
        
        // Apply status filter if specified
        if (status != null && !status.isEmpty()) {
            users = users.stream()
                    .filter(u -> u.getStatus().name().equalsIgnoreCase(status))
                    .toList();
        }
        
        PaginatedResponse<UserResponse> response = PaginatedResponse.<UserResponse>builder()
                .content(users)
                .totalElements(users.size())
                .totalPages(1)
                .size(size)
                .number(page)
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Get detailed information about a specific user")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        return keycloakAdminService.getUserById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new user
     */
    @PostMapping
    @Operation(summary = "Create user", description = "Create a new user in Keycloak and associated profile")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        String userId = keycloakAdminService.createUser(request);
        return keycloakAdminService.getUserById(userId)
                .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user))
                .orElse(ResponseEntity.status(HttpStatus.CREATED).build());
    }

    /**
     * Update an existing user
     */
    @PutMapping("/{userId}")
    @Operation(summary = "Update user", description = "Update user information and/or role")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserUpdateRequest request) {
        keycloakAdminService.updateUser(userId, request);
        return keycloakAdminService.getUserById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a user
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user", description = "Delete user from Keycloak and all associated data")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        keycloakAdminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reset user password
     */
    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Reset password", description = "Reset user's password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable String userId,
            @RequestBody PasswordResetRequest request) {
        keycloakAdminService.resetPassword(userId, request.getPassword(), request.isTemporary());
        return ResponseEntity.ok().build();
    }

    // Inner class for password reset request
    public static class PasswordResetRequest {
        private String password;
        private boolean temporary;

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public boolean isTemporary() { return temporary; }
        public void setTemporary(boolean temporary) { this.temporary = temporary; }
    }
}
