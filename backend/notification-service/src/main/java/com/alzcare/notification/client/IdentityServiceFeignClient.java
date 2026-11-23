package com.alzcare.notification.client;

import com.alzcare.notification.client.dto.IdentityUserResponse;
import com.alzcare.notification.client.dto.PaginatedUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for communicating with the Identity Service.
 * <p>Uses Eureka service discovery to resolve identity-service instances.</p>
 */
@FeignClient(name = "identity-service")
public interface IdentityServiceFeignClient {

    /**
     * Retrieves all users with optional filtering.
     *
     * @param role   optional role filter (PATIENT, CAREGIVER, DOCTOR, ADMIN)
     * @param active optional active status filter
     * @return paginated response containing users
     */
    @GetMapping("/api/v1/admin/users")
    PaginatedUserResponse getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active
    );

    /**
     * Retrieves a specific user by ID.
     *
     * @param userId the user ID
     * @return the user details
     */
    @GetMapping("/api/v1/admin/users/{id}")
    IdentityUserResponse getUserById(@PathVariable("id") String userId);
}