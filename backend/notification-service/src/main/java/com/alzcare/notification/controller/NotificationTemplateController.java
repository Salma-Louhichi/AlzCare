package com.alzcare.notification.controller;

import com.alzcare.notification.dto.request.NotificationTemplateRequest;
import com.alzcare.notification.dto.response.NotificationTemplateResponse;
import com.alzcare.notification.entities.NotificationTemplate;
import com.alzcare.notification.exception.ResourceNotFoundException;
import com.alzcare.notification.mapper.NotificationMapper;
import com.alzcare.notification.repositories.NotificationTemplateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for notification template management operations.
 * Provides endpoints for creating, reading, updating, and deleting notification templates.
 */
@RestController
@RequestMapping("/api/v1/notification-templates")
@Slf4j
@Tag(name = "Notification Templates", description = "Template management APIs")
public class NotificationTemplateController {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationMapper notificationMapper;

    /**
     * Constructs a new NotificationTemplateController with the required services.
     *
     * @param templateRepository the repository for template persistence
     * @param notificationMapper the mapper for notification conversions
     */
    public NotificationTemplateController(NotificationTemplateRepository templateRepository,
                                          NotificationMapper notificationMapper) {
        this.templateRepository = templateRepository;
        this.notificationMapper = notificationMapper;
    }

    /**
     * Get all notification templates.
     *
     * @return list of all templates
     */
    @GetMapping
    @Operation(summary = "List all templates",
            description = "Retrieve all available notification templates")
    public ResponseEntity<List<NotificationTemplateResponse>> getAllTemplates() {
        log.debug("Getting all notification templates");
        
        List<NotificationTemplate> templates = templateRepository.findAll();
        
        return ResponseEntity.ok(notificationMapper.toTemplateResponseList(templates));
    }

    /**
     * Get a template by its ID.
     *
     * @param id the template ID
     * @return the template if found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID",
            description = "Retrieve a specific template by its unique identifier")
    public ResponseEntity<NotificationTemplateResponse> getTemplateById(
            @Parameter(description = "Template ID")
            @PathVariable String id) {
        
        log.debug("Getting template by ID: {}", id);
        
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", id));
        
        return ResponseEntity.ok(notificationMapper.toResponse(template));
    }

    /**
     * Create a new notification template.
     *
     * @param request the template request
     * @return the created template
     */
    @PostMapping
    @Operation(summary = "Create template",
            description = "Create a new notification template")
    public ResponseEntity<NotificationTemplateResponse> createTemplate(
            @Valid @RequestBody NotificationTemplateRequest request) {
        
        log.info("Creating notification template with ID: {}", request.getId());
        
        // Check if template with same ID already exists
        if (templateRepository.existsById(request.getId())) {
            throw new IllegalArgumentException(
                    "Template with id '" + request.getId() + "' already exists");
        }
        
        // Convert request to entity
        NotificationTemplate template = notificationMapper.toEntity(request);
        
        // Save template
        NotificationTemplate savedTemplate = templateRepository.save(template);
        
        log.info("Created notification template: {}", savedTemplate.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationMapper.toResponse(savedTemplate));
    }

    /**
     * Update an existing notification template.
     *
     * @param id the template ID
     * @param request the template request with updated values
     * @return the updated template
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update template",
            description = "Update an existing notification template")
    public ResponseEntity<NotificationTemplateResponse> updateTemplate(
            @Parameter(description = "Template ID")
            @PathVariable String id,
            @Valid @RequestBody NotificationTemplateRequest request) {
        
        log.info("Updating notification template: {}", id);
        
        // Check if ID in path matches ID in request
        if (!id.equals(request.getId())) {
            throw new IllegalArgumentException(
                    "Template ID in path (" + id + ") does not match ID in request body (" + request.getId() + ")");
        }
        
        // Find existing template
        NotificationTemplate existingTemplate = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", id));
        
        // Update fields
        existingTemplate.setName(request.getName());
        existingTemplate.setType(request.getType());
        existingTemplate.setChannel(request.getChannel());
        existingTemplate.setSubjectTemplate(request.getSubjectTemplate());
        existingTemplate.setContentTemplate(request.getContentTemplate());
        existingTemplate.setDescription(request.getDescription());
        existingTemplate.setLanguage(request.getLanguage());
        existingTemplate.setVariables(request.getVariables());
        
        // Save updated template
        NotificationTemplate updatedTemplate = templateRepository.save(existingTemplate);
        
        log.info("Updated notification template: {}", updatedTemplate.getId());
        
        return ResponseEntity.ok(notificationMapper.toResponse(updatedTemplate));
    }

    /**
     * Delete a notification template.
     *
     * @param id the template ID
     * @return empty response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete template",
            description = "Permanently delete a notification template")
    public ResponseEntity<Void> deleteTemplate(
            @Parameter(description = "Template ID")
            @PathVariable String id) {
        
        log.info("Deleting notification template: {}", id);
        
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", id));
        
        templateRepository.delete(template);
        
        log.info("Deleted notification template: {}", id);
        
        return ResponseEntity.noContent().build();
    }
}
