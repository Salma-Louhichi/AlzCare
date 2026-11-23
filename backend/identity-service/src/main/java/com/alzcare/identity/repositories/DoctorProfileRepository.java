package com.alzcare.identity.repositories;

import com.alzcare.identity.entities.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link DoctorProfile} entities.
 * Provides CRUD operations and custom query methods for doctor profile data.
 */
@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {

    /**
     * Find a doctor profile by their user ID.
     *
     * @param userId the user ID
     * @return an Optional containing the doctor profile if found
     */
    Optional<DoctorProfile> findByUserId(UUID userId);

    /**
     * Find a doctor profile by their license number.
     *
     * @param licenseNumber the medical license number
     * @return an Optional containing the doctor profile if found
     */
    Optional<DoctorProfile> findByLicenseNumber(String licenseNumber);
}
