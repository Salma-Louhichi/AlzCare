package com.alzcare.notification.repositories;

import com.alzcare.notification.entities.NotificationTemplate;
import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link NotificationTemplate} documents.
 * <p>Provides CRUD operations and custom query methods for notification template data.</p>
 */
@Repository
public interface NotificationTemplateRepository extends MongoRepository<NotificationTemplate, String> {

    /**
     * Find a notification template by name and channel.
     *
     * @param name the template name
     * @param channel the notification channel
     * @return an Optional containing the template if found
     */
    Optional<NotificationTemplate> findByNameAndChannel(String name, NotificationChannel channel);

    /**
     * Find notification templates by type.
     *
     * @param type the notification type to filter by
     * @return a list of templates matching the type
     */
    List<NotificationTemplate> findByType(NotificationType type);
}
