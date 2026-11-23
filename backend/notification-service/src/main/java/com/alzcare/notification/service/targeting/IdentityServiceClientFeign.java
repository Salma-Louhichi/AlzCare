package com.alzcare.notification.service.targeting;

import com.alzcare.notification.client.IdentityServiceFeignClient;
import com.alzcare.notification.client.dto.IdentityUserResponse;
import com.alzcare.notification.client.dto.PaginatedUserResponse;
import com.alzcare.notification.dto.UserDto;
import com.alzcare.notification.exception.TargetResolutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service client for interacting with the Identity Service using OpenFeign.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityServiceClientFeign {

    private final IdentityServiceFeignClient feignClient;

    public List<UserDto> getAllActiveUsers() {
        log.debug("Fetching all active users from Identity Service");

        try {
            PaginatedUserResponse response = feignClient.getUsers(null, true);
            return convertToUserDtoList(response);
        } catch (Exception e) {
            log.error("Error calling Identity Service for all active users", e);
            throw new TargetResolutionException("Error calling Identity Service: " + e.getMessage(), e);
        }
    }

    public List<UserDto> getActiveUsersByRole(String role) {
        log.debug("Fetching active users with role '{}' from Identity Service", role);

        try {
            PaginatedUserResponse response = feignClient.getUsers(role, true);
            return convertToUserDtoList(response);
        } catch (Exception e) {
            log.error("Error calling Identity Service for users by role '{}'", role, e);
            throw new TargetResolutionException("Error calling Identity Service: " + e.getMessage(), e);
        }
    }

    public UserDto getUserById(String userId) {
        log.debug("Fetching user with id '{}' from Identity Service", userId);

        try {
            IdentityUserResponse response = feignClient.getUserById(userId);
            return convertToUserDto(response);
        } catch (Exception e) {
            log.warn("Failed to fetch user with id: {}", userId, e);
            return null;
        }
    }

    private List<UserDto> convertToUserDtoList(PaginatedUserResponse response) {
        if (response == null || response.getContent() == null) {
            return List.of();
        }
        return response.getContent().stream()
                .map(this::convertToUserDto)
                .collect(Collectors.toList());
    }

    private UserDto convertToUserDto(IdentityUserResponse userResponse) {
        UserDto dto = new UserDto();
        dto.setId(userResponse.getId());
        dto.setUsername(userResponse.getUsername());
        dto.setEmail(userResponse.getEmail());
        dto.setFirstName(userResponse.getFirstName());
        dto.setLastName(userResponse.getLastName());
        dto.setEnabled(userResponse.getEnabled() != null ? userResponse.getEnabled() : false);
        return dto;
    }
}
