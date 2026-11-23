package com.alzcare.notification.controller;

import com.alzcare.notification.dto.request.NotificationRequest;
import com.alzcare.notification.dto.response.NotificationCountResponse;
import com.alzcare.notification.dto.response.NotificationResponse;
import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.enums.NotificationStatus;
import com.alzcare.notification.enums.NotificationType;
import com.alzcare.notification.exception.ResourceNotFoundException;
import com.alzcare.notification.mapper.NotificationMapper;
import com.alzcare.notification.repositories.NotificationRepository;
import com.alzcare.notification.service.NotificationRoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST controller for notification management operations.
 * Provides endpoints for creating, reading, updating, and managing notifications.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Slf4j
@Tag(name = "Notifications", description = "Notification management APIs")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationRoutingService routingService;

    /**
     * Constructs a new NotificationController with the required services.
     *
     * @param notificationRepository the repository for notification persistence
     * @param notificationMapper the mapper for notification conversions
     * @param routingService the service for routing notifications
     */
    public NotificationController(NotificationRepository notificationRepository,
                                  NotificationMapper notificationMapper,
                                  NotificationRoutingService routingService) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.routingService = routingService;
    }

    /**
     * Get all notifications with optional filters.
     *
     * @param userId optional user ID filter
     * @param status optional status filter
     * @param type optional type filter
     * @param pageable pagination information
     * @return paginated list of notifications
     */
    @GetMapping
    @Operation(summary = "Get all notifications with filters",
            description = "Retrieve paginated notifications filtered by user ID, status, and/or type")
    public ResponseEntity<Page<NotificationResponse>> getAllNotifications(
            @Parameter(description = "Filter by recipient user ID")
            @RequestParam(required = false) String userId,
            @Parameter(description = "Filter by notification status")
            @RequestParam(required = false) NotificationStatus status,
            @Parameter(description = "Filter by notification type")
            @RequestParam(required = false) NotificationType type,
            Pageable pageable) {
        
        log.debug("Getting notifications with filters - userId: {}, status: {}, type: {}", userId, status, type);
        
        // Build example matcher for dynamic filtering
        Notification probe = Notification.builder()
                .recipientId(userId)
                .status(status)
                .type(type)
                .build();
        
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withIgnoreCase();
        
        Example<Notification> example = Example.of(probe, matcher);
        Page<Notification> notifications = notificationRepository.findAll(example, pageable);
        
        return ResponseEntity.ok(notifications.map(notificationMapper::toResponse));
    }

    /**
     * Get a notification by its ID.
     *
     * @param id the notification ID
     * @return the notification if found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID",
            description = "Retrieve a specific notification by its unique identifier")
    public ResponseEntity<NotificationResponse> getNotificationById(
            @Parameter(description = "Notification ID")
            @PathVariable String id) {
        
        log.debug("Getting notification by ID: {}", id);
        
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        
        return ResponseEntity.ok(notificationMapper.toResponse(notification));
    }

    /**
     * Get all notifications for a specific user.
     *
     * @param userId the user ID
     * @param status optional status filter
     * @param unread optional unread filter (true=unread, false=read, null=no filter)
     * @param hours optional filter for notifications from last N hours
     * @param days optional filter for notifications from last N days
     * @param pageable pagination information
     * @return paginated list of user's notifications
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's notifications",
            description = "Retrieve paginated notifications for a specific user with optional date filters")
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @Parameter(description = "User ID")
            @PathVariable String userId,
            @Parameter(description = "Filter by notification status")
            @RequestParam(required = false) NotificationStatus status,
            @Parameter(description = "Filter by unread status (true=unread, false=read, null=no filter)")
            @RequestParam(required = false) Boolean unread,
            @Parameter(description = "Filter by notifications from last N hours")
            @RequestParam(required = false) Integer hours,
            @Parameter(description = "Filter by notifications from last N days")
            @RequestParam(required = false) Integer days,
            Pageable pageable) {
        
        log.debug("Getting notifications for user: {} with status: {}, unread: {}, hours: {}, days: {}", 
                userId, status, unread, hours, days);
        
        // Calculate cutoff date based on hours or days filter
        java.time.Instant cutoffDate = null;
        java.time.Instant now = java.time.Instant.now();
        
        if (hours != null && hours > 0) {
            // Filter by hours (for bell)
            cutoffDate = now.minus(java.time.Duration.ofHours(hours));
        } else if (days != null && days > 0) {
            // Filter by days (for center)
            cutoffDate = now.minus(java.time.Duration.ofDays(days));
        }
        
        // Retrieve notifications based on filters
        java.util.List<Notification> allNotifications;
        if (cutoffDate != null) {
            // Use date-filtered query
            if (status != null) {
                allNotifications = notificationRepository.findByRecipientIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                        userId, status, cutoffDate);
            } else if (unread != null) {
                if (unread) {
                    // Get unread notifications (status != READ)
                    allNotifications = notificationRepository.findByRecipientIdAndStatusNotAndCreatedAtAfterOrderByCreatedAtDesc(
                            userId, NotificationStatus.READ, cutoffDate);
                } else {
                    // Get read notifications (status == READ)
                    allNotifications = notificationRepository.findByRecipientIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                            userId, NotificationStatus.READ, cutoffDate);
                }
            } else {
                allNotifications = notificationRepository.findByRecipientIdAndCreatedAtAfterOrderByCreatedAtDesc(
                        userId, cutoffDate);
            }
        } else {
            // Use non-date-filtered queries for backward compatibility
            if (status != null) {
                allNotifications = notificationRepository.findByRecipientIdAndStatus(userId, status);
            } else if (unread != null) {
                if (unread) {
                    // Get unread notifications (status != READ)
                    allNotifications = notificationRepository.findByRecipientIdAndStatusNot(userId, NotificationStatus.READ);
                } else {
                    // Get read notifications (status == READ)
                    allNotifications = notificationRepository.findByRecipientIdAndStatus(userId, NotificationStatus.READ);
                }
            } else {
                allNotifications = notificationRepository.findByRecipientId(userId, Pageable.unpaged());
            }
        }
        
        // Ensure list is sorted by createdAt desc for consistency
        if (cutoffDate == null) {
            allNotifications.sort((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()));
        }
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allNotifications.size());
        java.util.List<Notification> pageContent = allNotifications.subList(start, end);
        
        Page<Notification> notifications = new PageImpl<>(pageContent, pageable, allNotifications.size());
        
        return ResponseEntity.ok(notifications.map(notificationMapper::toResponse));
    }

    /**
     * Get the count of unread notifications for a user.
     *
     * @param userId the user ID
     * @return the unread and total notification counts
     */
    @GetMapping("/user/{userId}/unread/count")
    @Operation(summary = "Get unread notification count for user",
            description = "Retrieve the count of unread and total notifications for a specific user")
    public ResponseEntity<NotificationCountResponse> getUnreadCount(
            @Parameter(description = "User ID")
            @PathVariable String userId) {
        
        log.debug("Getting unread count for user: {}", userId);
        
        long unreadCount = notificationRepository.countByRecipientIdAndStatusNot(userId, NotificationStatus.READ);
        long totalCount = notificationRepository.findByRecipientId(userId, Pageable.unpaged()).size();
        
        NotificationCountResponse response = notificationMapper.toCountResponse(userId, unreadCount, totalCount);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Send a manual notification.
     *
     * @param request the notification request
     * @return the created notification
     */
    @PostMapping
    @Operation(summary = "Send manual notification",
            description = "Create and send a new notification immediately")
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody NotificationRequest request) {
        
        log.info("Sending manual notification to user: {}", request.getRecipientId());
        
        // Convert request to entity
        Notification notification = notificationMapper.toEntity(request);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);
        
        // Save notification
        Notification savedNotification = notificationRepository.save(notification);
        
        // Route notification for delivery
        routingService.routeNotification(savedNotification);
        
        // Reload to get updated status
        savedNotification = notificationRepository.findById(savedNotification.getId())
                .orElse(savedNotification);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationMapper.toResponse(savedNotification));
    }

    /**
     * Retry a failed notification.
     *
     * @param id the notification ID
     * @return the updated notification
     */
    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry failed notification",
            description = "Retry sending a notification that previously failed")
    public ResponseEntity<NotificationResponse> retryNotification(
            @Parameter(description = "Notification ID")
            @PathVariable String id) {
        
        log.info("Retrying notification: {}", id);
        
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        
        // Check if notification is in a retryable state
        if (notification.getStatus() != NotificationStatus.FAILED && 
            notification.getStatus() != NotificationStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Cannot retry notification with status: " + notification.getStatus());
        }
        
        // Reset to pending for retry
        notification.setStatus(NotificationStatus.PENDING);
        notification.setErrorMessage(null);
        notification = notificationRepository.save(notification);
        
        // Route for delivery
        routingService.routeNotification(notification);
        
        // Reload to get updated status
        notification = notificationRepository.findById(id).orElse(notification);
        
        return ResponseEntity.ok(notificationMapper.toResponse(notification));
    }

    /**
     * Mark a notification as read.
     *
     * @param id the notification ID
     * @return the updated notification
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read",
            description = "Mark a specific notification as read by the user")
    public ResponseEntity<NotificationResponse> markAsRead(
            @Parameter(description = "Notification ID")
            @PathVariable String id) {
        
        log.debug("Marking notification as read: {}", id);
        
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(Instant.now());
        
        Notification updatedNotification = notificationRepository.save(notification);
        
        return ResponseEntity.ok(notificationMapper.toResponse(updatedNotification));
    }

    /**
     * Mark all notifications as read for a user.
     *
     * @param userId the user ID
     * @return empty response
     */
    @PutMapping("/user/{userId}/mark-all-read")
    @Operation(summary = "Mark all notifications as read for user",
            description = "Mark all notifications as read for a specific user")
    public ResponseEntity<Void> markAllAsRead(
            @Parameter(description = "User ID")
            @PathVariable String userId) {
        
        log.info("Marking all notifications as read for user: {}", userId);
        
        java.util.List<Notification> unreadNotifications = notificationRepository
                .findByRecipientIdAndStatusNot(userId, NotificationStatus.READ);
        
        for (Notification notification : unreadNotifications) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(Instant.now());
        }
        
        notificationRepository.saveAll(unreadNotifications);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Delete a notification.
     *
     * @param id the notification ID
     * @return empty response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification",
            description = "Permanently delete a notification")
    public ResponseEntity<Void> deleteNotification(
            @Parameter(description = "Notification ID")
            @PathVariable String id) {
        
        log.info("Deleting notification: {}", id);
        
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        
        notificationRepository.delete(notification);
        
        return ResponseEntity.noContent().build();
    }
}
