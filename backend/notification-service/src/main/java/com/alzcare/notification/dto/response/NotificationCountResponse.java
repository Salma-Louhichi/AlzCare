package com.alzcare.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for notification count data.
 * <p>Returns unread and total notification counts for a user.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCountResponse {

    private String userId;
    private Long unreadCount;
    private Long totalCount;
}
