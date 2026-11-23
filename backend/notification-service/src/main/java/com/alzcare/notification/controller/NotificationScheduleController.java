package com.alzcare.notification.controller;

import com.alzcare.notification.dto.request.CreateScheduleRequest;
import com.alzcare.notification.dto.request.UpdateScheduleRequest;
import com.alzcare.notification.dto.response.ScheduleResponse;
import com.alzcare.notification.entities.ScheduledNotificationRule;
import com.alzcare.notification.enums.ScheduleType;
import com.alzcare.notification.exception.ResourceNotFoundException;
import com.alzcare.notification.repositories.NotificationScheduleRepository;
import com.alzcare.notification.service.dynamic.DynamicSchedulerService;
import com.alzcare.notification.service.execution.ScheduleExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing notification schedules.
 * <p>Provides endpoints for creating, reading, updating, and deleting scheduled
 * notification rules, as well as toggling active status and manual triggering.</p>
 * <p>All operations require ADMIN role.</p>
 */
@RestController
@RequestMapping("/api/v1/schedules")
@Slf4j
@Tag(name = "Notification Schedules", description = "Schedule management APIs for dynamic notification scheduling")
public class NotificationScheduleController {

    private final NotificationScheduleRepository scheduleRepository;
    private final DynamicSchedulerService schedulerService;
    private final ScheduleExecutionService executionService;

    /**
     * Constructs a new NotificationScheduleController with the required services.
     *
     * @param scheduleRepository the repository for schedule persistence
     * @param schedulerService   the service for dynamic scheduling operations
     * @param executionService   the service for manual schedule execution
     */
    public NotificationScheduleController(NotificationScheduleRepository scheduleRepository,
                                          DynamicSchedulerService schedulerService,
                                          ScheduleExecutionService executionService) {
        this.scheduleRepository = scheduleRepository;
        this.schedulerService = schedulerService;
        this.executionService = executionService;
    }

    /**
     * Creates a new notification schedule.
     *
     * @param request the create schedule request
     * @return the created schedule response
     */
    @PostMapping
    @Operation(summary = "Create schedule",
            description = "Create a new notification schedule with interval, cron, or one-time configuration")
    public ResponseEntity<ScheduleResponse> createSchedule(
            @Valid @RequestBody CreateScheduleRequest request) {
        log.info("Creating new notification schedule: {}", request.getName());

        ScheduledNotificationRule rule = mapToEntity(request);
        rule.setExecutionCount(0);
        rule.setNextExecutionTime(calculateNextExecution(rule));

        ScheduledNotificationRule savedRule = scheduleRepository.save(rule);

        // Schedule the rule if it's active
        if (savedRule.isActive()) {
            try {
                schedulerService.scheduleRule(savedRule);
            } catch (Exception e) {
                log.error("Failed to schedule rule [{}]: {}", savedRule.getId(), e.getMessage());
            }
        }

        log.info("Created notification schedule: {}", savedRule.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToResponse(savedRule));
    }

    /**
     * Lists all schedules with optional filtering and pagination.
     *
     * @param targetRole optional filter by target role
     * @param active     optional filter by active status
     * @param pageable   pagination information
     * @return page of schedule responses
     */
    @GetMapping
    @Operation(summary = "List schedules",
            description = "Retrieve all notification schedules with optional filtering and pagination")
    public ResponseEntity<Page<ScheduleResponse>> listSchedules(
            @Parameter(description = "Filter by target role (PATIENT, CAREGIVER, DOCTOR, ALL)")
            @RequestParam(required = false) String targetRole,
            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        log.debug("Listing schedules with targetRole={}, active={}", targetRole, active);

        Page<ScheduledNotificationRule> rules;

        if (targetRole != null && active != null) {
            rules = scheduleRepository.findAll(
                    Example.of(
                            ScheduledNotificationRule.builder()
                                    .targetRole(targetRole)
                                    .active(active)
                                    .build(),
                            ExampleMatcher.matching().withIgnoreNullValues()
                    ), pageable);
        } else if (targetRole != null) {
            rules = scheduleRepository.findAll(
                    Example.of(
                            ScheduledNotificationRule.builder()
                                    .targetRole(targetRole)
                                    .build(),
                            ExampleMatcher.matching().withIgnoreNullValues()
                    ), pageable);
        } else if (active != null) {
            rules = scheduleRepository.findAll(
                    Example.of(
                            ScheduledNotificationRule.builder()
                                    .active(active)
                                    .build(),
                            ExampleMatcher.matching().withIgnoreNullValues()
                    ), pageable);
        } else {
            rules = scheduleRepository.findAll(pageable);
        }

        return ResponseEntity.ok(rules.map(this::mapToResponse));
    }

    /**
     * Gets a schedule by its ID.
     *
     * @param id the schedule ID
     * @return the schedule response
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get schedule",
            description = "Retrieve a specific notification schedule by its ID")
    public ResponseEntity<ScheduleResponse> getSchedule(
            @Parameter(description = "Schedule ID")
            @PathVariable String id) {
        log.debug("Getting schedule: {}", id);

        ScheduledNotificationRule rule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledNotificationRule", id));

        return ResponseEntity.ok(mapToResponse(rule));
    }

    /**
     * Updates an existing notification schedule.
     *
     * @param id      the schedule ID
     * @param request the update request
     * @return the updated schedule response
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update schedule",
            description = "Update an existing notification schedule")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @Parameter(description = "Schedule ID")
            @PathVariable String id,
            @Valid @RequestBody UpdateScheduleRequest request) {
        log.info("Updating notification schedule: {}", id);

        ScheduledNotificationRule existingRule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledNotificationRule", id));

        // Update fields
        existingRule.setName(request.getName());
        existingRule.setDescription(request.getDescription());
        existingRule.setTargetRole(request.getTargetRole());
        existingRule.setTargetUserIds(request.getTargetUserIds());
        existingRule.setTitleTemplate(request.getTitleTemplate());
        existingRule.setMessageTemplate(request.getMessageTemplate());
        existingRule.setType(request.getType());
        existingRule.setPriority(request.getPriority());
        existingRule.setChannels(request.getChannels());
        existingRule.setScheduleType(request.getScheduleType());
        existingRule.setCronExpression(request.getCronExpression());
        existingRule.setIntervalMinutes(request.getIntervalMinutes());
        existingRule.setStartDate(request.getStartDate());
        existingRule.setEndDate(request.getEndDate());
        existingRule.setTimezone(request.getTimezone());
        existingRule.setActive(request.isActive());
        existingRule.setNextExecutionTime(calculateNextExecution(existingRule));

        ScheduledNotificationRule updatedRule = scheduleRepository.save(existingRule);

        // Reschedule the rule
        try {
            if (updatedRule.isActive()) {
                schedulerService.rescheduleRule(updatedRule);
            } else {
                schedulerService.cancelSchedule(updatedRule.getId());
            }
        } catch (Exception e) {
            log.error("Failed to reschedule rule [{}]: {}", updatedRule.getId(), e.getMessage());
        }

        log.info("Updated notification schedule: {}", updatedRule.getId());
        return ResponseEntity.ok(mapToResponse(updatedRule));
    }

    /**
     * Deletes a notification schedule.
     *
     * @param id the schedule ID
     * @return empty response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete schedule",
            description = "Permanently delete a notification schedule")
    public ResponseEntity<Void> deleteSchedule(
            @Parameter(description = "Schedule ID")
            @PathVariable String id) {
        log.info("Deleting notification schedule: {}", id);

        ScheduledNotificationRule rule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledNotificationRule", id));

        // Cancel any active schedule
        schedulerService.cancelSchedule(id);

        scheduleRepository.delete(rule);

        log.info("Deleted notification schedule: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggles the active status of a schedule.
     *
     * @param id the schedule ID
     * @return the updated schedule response
     */
    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Toggle schedule",
            description = "Toggle the active/inactive status of a notification schedule")
    public ResponseEntity<ScheduleResponse> toggleSchedule(
            @Parameter(description = "Schedule ID")
            @PathVariable String id) {
        log.info("Toggling schedule: {}", id);

        ScheduledNotificationRule rule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledNotificationRule", id));

        boolean newActiveState = !rule.isActive();
        rule.setActive(newActiveState);

        // Update next execution time when activating
        if (newActiveState) {
            rule.setNextExecutionTime(calculateNextExecution(rule));
        }

        ScheduledNotificationRule updatedRule = scheduleRepository.save(rule);

        // Update scheduler
        try {
            if (newActiveState) {
                schedulerService.scheduleRule(updatedRule);
            } else {
                schedulerService.cancelSchedule(id);
            }
        } catch (Exception e) {
            log.error("Failed to update schedule state for rule [{}]: {}", id, e.getMessage());
        }

        log.info("Schedule [{}] is now {}", id, newActiveState ? "active" : "inactive");
        return ResponseEntity.ok(mapToResponse(updatedRule));
    }

    /**
     * Manually triggers a schedule execution.
     *
     * @param id the schedule ID
     * @return response with execution status
     */
    @PostMapping("/{id}/trigger")
    @Operation(summary = "Trigger schedule",
            description = "Manually trigger execution of a notification schedule")
    public ResponseEntity<Map<String, String>> triggerSchedule(
            @Parameter(description = "Schedule ID")
            @PathVariable String id) {
        log.info("Manually triggering schedule: {}", id);

        ScheduledNotificationRule rule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledNotificationRule", id));

        if (!rule.isActive()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot trigger inactive schedule", "scheduleId", id));
        }

        try {
            executionService.executeSchedule(id);
            log.info("Successfully triggered schedule: {}", id);
            return ResponseEntity.ok(Map.of(
                    "message", "Schedule triggered successfully",
                    "scheduleId", id,
                    "status", "executing"));
        } catch (Exception e) {
            log.error("Failed to trigger schedule [{}]: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to trigger schedule: " + e.getMessage(), "scheduleId", id));
        }
    }

    /**
     * Maps a CreateScheduleRequest to a ScheduledNotificationRule entity.
     */
    private ScheduledNotificationRule mapToEntity(CreateScheduleRequest request) {
        return ScheduledNotificationRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .targetRole(request.getTargetRole())
                .targetUserIds(request.getTargetUserIds())
                .titleTemplate(request.getTitleTemplate())
                .messageTemplate(request.getMessageTemplate())
                .type(request.getType())
                .priority(request.getPriority())
                .channels(request.getChannels())
                .scheduleType(request.getScheduleType())
                .cronExpression(request.getCronExpression())
                .intervalMinutes(request.getIntervalMinutes())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .timezone(request.getTimezone())
                .active(request.isActive())
                .build();
    }

    /**
     * Maps a ScheduledNotificationRule entity to a ScheduleResponse DTO.
     */
    private ScheduleResponse mapToResponse(ScheduledNotificationRule rule) {
        return ScheduleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .description(rule.getDescription())
                .targetRole(rule.getTargetRole())
                .targetUserIds(rule.getTargetUserIds())
                .titleTemplate(rule.getTitleTemplate())
                .messageTemplate(rule.getMessageTemplate())
                .type(rule.getType())
                .priority(rule.getPriority())
                .channels(rule.getChannels())
                .scheduleType(rule.getScheduleType())
                .cronExpression(rule.getCronExpression())
                .intervalMinutes(rule.getIntervalMinutes())
                .startDate(rule.getStartDate())
                .endDate(rule.getEndDate())
                .timezone(rule.getTimezone())
                .active(rule.isActive())
                .createdBy(rule.getCreatedBy())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .lastExecutedAt(rule.getLastExecutedAt())
                .executionCount(rule.getExecutionCount())
                .nextExecutionTime(rule.getNextExecutionTime())
                .build();
    }

    /**
     * Calculates the next execution time for a rule based on its schedule type.
     */
    private Instant calculateNextExecution(ScheduledNotificationRule rule) {
        if (!rule.isActive()) {
            return null;
        }

        Instant now = Instant.now();

        // Check if start date is in the future
        if (rule.getStartDate() != null && rule.getStartDate().isAfter(now)) {
            return rule.getStartDate();
        }

        // For one-time schedules that have passed their start date, return null
        if (rule.getScheduleType() == ScheduleType.ONE_TIME) {
            return rule.getStartDate();
        }

        // For interval-based schedules
        if (rule.getScheduleType() == ScheduleType.INTERVAL && rule.getIntervalMinutes() != null) {
            return now.plusSeconds(rule.getIntervalMinutes() * 60L);
        }

        // For cron-based schedules
        if (rule.getScheduleType() == ScheduleType.CRON && rule.getCronExpression() != null) {
            try {
                org.springframework.scheduling.support.CronExpression cronExpression =
                        org.springframework.scheduling.support.CronExpression.parse(rule.getCronExpression());
                return cronExpression.next(now);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid cron expression for rule: {}", rule.getId());
                return null;
            }
        }

        return null;
    }
}
