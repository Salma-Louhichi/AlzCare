package com.alzcare.notification.service.targeting;

import com.alzcare.notification.dto.UserDto;
import com.alzcare.notification.entities.ScheduledNotificationRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolves target users for scheduled notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TargetUserResolver {

    private final IdentityServiceClientFeign identityServiceClient;
    private static final String ROLE_ALL = "ALL";

    public List<String> resolveTargetUsers(ScheduledNotificationRule rule) {
        log.debug("Resolving target users for rule: {}", rule.getId());

        if (!CollectionUtils.isEmpty(rule.getTargetUserIds())) {
            log.debug("Using explicit target user IDs: {}", rule.getTargetUserIds());
            return rule.getTargetUserIds();
        }

        List<UserDto> users;
        String targetRole = rule.getTargetRole();

        if (ROLE_ALL.equalsIgnoreCase(targetRole)) {
            log.debug("Target role is 'ALL', fetching all active users from Identity Service");
            users = identityServiceClient.getAllActiveUsers();
        } else {
            log.debug("Target role is '{}', fetching users by role from Identity Service", targetRole);
            users = identityServiceClient.getActiveUsersByRole(targetRole);
        }

        List<String> userIds = users.stream()
                .map(UserDto::getId)
                .collect(Collectors.toList());

        log.debug("Resolved {} target users for rule: {}", userIds.size(), rule.getId());
        return userIds;
    }
}
