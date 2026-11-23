package com.alzcare.notification.repositories;

import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.enums.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for managing {@link Notification} documents.
 * <p>Provides CRUD operations and custom query methods for notification data.</p>
 */
@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    /**
     * Find notifications by recipient ID and status.
     *
     * @param recipientId the recipient user ID
     * @param status the notification status to filter by
     * @return a list of notifications matching the criteria
     */
    List<Notification> findByRecipientIdAndStatus(String recipientId, NotificationStatus status);

    /**
     * Find notifications by recipient ID with pagination.
     *
     * @param recipientId the recipient user ID
     * @param pageable pagination information
     * @return a list of notifications for the recipient, paginated
     */
    List<Notification> findByRecipientId(String recipientId, Pageable pageable);

    /**
     * Find notifications by recipient ID and creation date, sorted by creation date descending.
     * <p>Used for date-based filtering (e.g., last N hours or days).</p>
     *
     * @param recipientId the recipient user ID
     * @param createdAfter the cutoff date to filter notifications created after
     * @return a list of notifications matching the criteria, sorted by createdAt desc
     */
    List<Notification> findByRecipientIdAndCreatedAtAfterOrderByCreatedAtDesc(String recipientId, Instant createdAfter);

    /**
     * Find notifications by recipient ID, status, and creation date, sorted by creation date descending.
     * <p>Used for date-based filtering with status filter.</p>
     *
     * @param recipientId the recipient user ID
     * @param status the notification status to filter by
     * @param createdAfter the cutoff date to filter notifications created after
     * @return a list of notifications matching the criteria, sorted by createdAt desc
     */
    List<Notification> findByRecipientIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            String recipientId, NotificationStatus status, Instant createdAfter);

    /**
     * Count notifications by recipient ID and status.
     *
     * @param recipientId the recipient user ID
     * @param status the notification status to filter by
     * @return the count of matching notifications
     */
    long countByRecipientIdAndStatus(String recipientId, NotificationStatus status);

    /**
     * Count notifications by recipient ID where status is not equal to the specified status.
     *
     * @param recipientId the recipient user ID
     * @param status the notification status to exclude
     * @return the count of matching notifications
     */
    long countByRecipientIdAndStatusNot(String recipientId, NotificationStatus status);

    /**
     * Find notifications by recipient ID where status is not equal to the specified status.
     *
     * @param recipientId the recipient user ID
     * @param status the notification status to exclude
     * @return a list of notifications matching the criteria
     */
    List<Notification> findByRecipientIdAndStatusNot(String recipientId, NotificationStatus status);

    /**
     * Find notifications by recipient ID where status is not equal to the specified status and created after specified date.
     *
     * @param recipientId the recipient user ID
     * @param status the notification status to exclude
     * @param createdAfter the cutoff date to filter notifications created after
     * @return a list of notifications matching the criteria, sorted by createdAt desc
     */
    List<Notification> findByRecipientIdAndStatusNotAndCreatedAtAfterOrderByCreatedAtDesc(
            String recipientId, NotificationStatus status, Instant createdAfter);

    /**
     * Find notifications by status with retry count less than specified value.
     * <p>Used for retrying failed notifications.</p>
     *
     * @param status the notification status to filter by
     * @param maxRetries the maximum retry count threshold
     * @return a list of notifications eligible for retry
     */
    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);
}
