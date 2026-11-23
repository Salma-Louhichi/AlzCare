package com.alzcare.notification.client.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO representing a paginated response from the Identity Service.
 * Used by Feign client for deserializing paginated user data.
 */
@Data
public class PaginatedUserResponse {
    private List<IdentityUserResponse> content;
    private long totalElements;
    private int totalPages;
    private int size;
    private int number;
}