package com.alzcare.notification.mapper;

import com.alzcare.notification.dto.request.NotificationRequest;
import com.alzcare.notification.dto.request.NotificationTemplateRequest;
import com.alzcare.notification.dto.response.NotificationCountResponse;
import com.alzcare.notification.dto.response.NotificationResponse;
import com.alzcare.notification.dto.response.NotificationTemplateResponse;
import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.entities.NotificationTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for Notification and NotificationTemplate entities and DTOs.
 * <p>Handles conversion between entities, request DTOs, and response DTOs.</p>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    /**
     * Converts NotificationRequest DTO to Notification entity.
     *
     * @param request the create request DTO
     * @return the entity
     */
    Notification toEntity(NotificationRequest request);

    /**
     * Converts Notification entity to NotificationResponse DTO.
     * Maps enum fields to their string names.
     *
     * @param entity the entity to convert
     * @return the response DTO
     */
    @Mapping(target = "type", expression = "java(entity.getType() != null ? entity.getType().name() : null)")
    @Mapping(target = "channel", expression = "java(entity.getChannel() != null ? entity.getChannel().name() : null)")
    @Mapping(target = "status", expression = "java(entity.getStatus() != null ? entity.getStatus().name() : null)")
    @Mapping(target = "priority", expression = "java(entity.getPriority() != null ? entity.getPriority().name() : null)")
    NotificationResponse toResponse(Notification entity);

    /**
     * Converts a list of Notification entities to a list of NotificationResponse DTOs.
     *
     * @param entities the entities to convert
     * @return the list of response DTOs
     */
    List<NotificationResponse> toResponseList(List<Notification> entities);

    /**
     * Converts NotificationTemplateRequest DTO to NotificationTemplate entity.
     *
     * @param request the create request DTO
     * @return the entity
     */
    NotificationTemplate toEntity(NotificationTemplateRequest request);

    /**
     * Converts NotificationTemplate entity to NotificationTemplateResponse DTO.
     * Maps enum fields to their string names.
     *
     * @param entity the entity to convert
     * @return the response DTO
     */
    @Mapping(target = "type", expression = "java(entity.getType() != null ? entity.getType().name() : null)")
    @Mapping(target = "channel", expression = "java(entity.getChannel() != null ? entity.getChannel().name() : null)")
    NotificationTemplateResponse toResponse(NotificationTemplate entity);

    /**
     * Converts a list of NotificationTemplate entities to a list of NotificationTemplateResponse DTOs.
     *
     * @param entities the entities to convert
     * @return the list of response DTOs
     */
    List<NotificationTemplateResponse> toTemplateResponseList(List<NotificationTemplate> entities);

    /**
     * Creates a NotificationCountResponse for a user.
     *
     * @param userId the user ID
     * @param unreadCount the unread notification count
     * @param totalCount the total notification count
     * @return the count response DTO
     */
    default NotificationCountResponse toCountResponse(String userId, Long unreadCount, Long totalCount) {
        return NotificationCountResponse.builder()
                .userId(userId)
                .unreadCount(unreadCount)
                .totalCount(totalCount)
                .build();
    }
}
