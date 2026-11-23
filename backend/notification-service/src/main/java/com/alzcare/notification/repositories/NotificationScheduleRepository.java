package com.alzcare.notification.repositories;

import com.alzcare.notification.entities.ScheduledNotificationRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for managing {@link ScheduledNotificationRule} documents.
 * <p>Provides CRUD operations and custom query methods for scheduled notification
 * rules, enabling efficient scheduling and execution of recurring notifications.</p>
 */
@Repository
public interface NotificationScheduleRepository extends MongoRepository<ScheduledNotificationRule, String> {

    /**
     * Find all active scheduled notification rules.
     *
     * @return a list of active rules
     */
    List<ScheduledNotificationRule> findByActiveTrue();

    /**
     * Find active rules that are due for execution (next execution time is before the given time).
     *
     * @param time the reference time to compare against
     * @return a list of rules ready for execution
     */
    List<ScheduledNotificationRule> findByActiveTrueAndNextExecutionTimeBefore(Instant time);

    /**
     * Find scheduled notification rules by target role.
     *
     * @param targetRole the target role to filter by (PATIENT, CAREGIVER, DOCTOR, ALL)
     * @return a list of rules targeting the specified role
     */
    List<ScheduledNotificationRule> findByTargetRole(String targetRole);

    /**
     * Find scheduled notification rules by the user who created them.
     *
     * @param createdBy the user ID of the creator
     * @return a list of rules created by the specified user
     */
    List<ScheduledNotificationRule> findByCreatedBy(String createdBy);

    /**
     * Count the number of active scheduled notification rules.
     *
     * @return the count of active rules
     */
    long countByActiveTrue();
}
