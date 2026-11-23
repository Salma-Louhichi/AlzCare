package com.alzcare.identity.repositories;

import com.alzcare.identity.entities.CaregiverProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link CaregiverProfile} entities.
 * Provides CRUD operations and custom query methods for caregiver profile data.
 */
@Repository
public interface CaregiverProfileRepository extends JpaRepository<CaregiverProfile, UUID> {

    /**
     * Find a caregiver profile by their user ID.
     *
     * @param userId the user ID
     * @return an Optional containing the caregiver profile if found
     */
    Optional<CaregiverProfile> findByUserId(UUID userId);
}
