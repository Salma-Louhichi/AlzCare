package com.alzcare.identity.services;

import com.alzcare.identity.dtos.requests.CaregiverCreateRequest;
import com.alzcare.identity.dtos.requests.CaregiverUpdateRequest;
import com.alzcare.identity.dtos.responses.CaregiverProfileResponse;
import com.alzcare.identity.entities.CaregiverProfile;
import com.alzcare.identity.exceptions.CaregiverNotFoundException;
import com.alzcare.identity.mappers.CaregiverProfileMapper;
import com.alzcare.identity.repositories.CaregiverProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing caregiver profiles.
 * Provides business logic for retrieving and updating caregiver profile information.
 */
@Service
public class CaregiverProfileService {

    private final CaregiverProfileRepository caregiverProfileRepository;
    private final CaregiverProfileMapper caregiverProfileMapper;

    /**
     * Constructs a new CaregiverProfileService with required dependencies.
     *
     * @param caregiverProfileRepository the repository for caregiver profile data access
     * @param caregiverProfileMapper     the mapper for converting between entities and DTOs
     */
    public CaregiverProfileService(CaregiverProfileRepository caregiverProfileRepository,
                                   CaregiverProfileMapper caregiverProfileMapper) {
        this.caregiverProfileRepository = caregiverProfileRepository;
        this.caregiverProfileMapper = caregiverProfileMapper;
    }

    /**
     * Creates a new caregiver profile.
     *
     * @param request the caregiver creation request containing profile details
     * @return the created caregiver profile response
     * @throws IllegalArgumentException if a caregiver with the same userId already exists
     */
    @Transactional
    public CaregiverProfileResponse createCaregiverProfile(CaregiverCreateRequest request) {
        caregiverProfileRepository.findByUserId(request.getUserId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            String.format("Caregiver profile already exists with userId: %s", request.getUserId()));
                });

        CaregiverProfile caregiverProfile = caregiverProfileMapper.toEntity(request);
        CaregiverProfile savedProfile = caregiverProfileRepository.saveAndFlush(caregiverProfile);
        return caregiverProfileMapper.toResponse(savedProfile);
    }

    /**
     * Retrieves a caregiver profile by their internal ID.
     *
     * @param id the caregiver profile UUID
     * @return the caregiver profile response
     * @throws CaregiverNotFoundException if no caregiver is found with the given ID
     */
    @Transactional(readOnly = true)
    public CaregiverProfileResponse getCaregiverById(UUID id) {
        CaregiverProfile caregiverProfile = caregiverProfileRepository.findById(id)
                .orElseThrow(() -> new CaregiverNotFoundException(id));
        return caregiverProfileMapper.toResponse(caregiverProfile);
    }

    /**
     * Retrieves a caregiver profile by their user ID.
     *
     * @param userId the user ID (typically from Keycloak)
     * @return the caregiver profile response
     * @throws CaregiverNotFoundException if no caregiver is found with the given user ID
     */
    @Transactional(readOnly = true)
    public CaregiverProfileResponse getCaregiverByUserId(UUID userId) {
        CaregiverProfile caregiverProfile = caregiverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CaregiverNotFoundException("Caregiver profile not found with userId: " + userId));
        return caregiverProfileMapper.toResponse(caregiverProfile);
    }

    /**
     * Updates an existing caregiver profile.
     * Only non-null fields from the request will be updated.
     *
     * @param id      the caregiver profile UUID to update
     * @param request the update request containing fields to modify
     * @return the updated caregiver profile response
     * @throws CaregiverNotFoundException if no caregiver is found with the given ID
     */
    @Transactional
    public CaregiverProfileResponse updateCaregiver(UUID id, CaregiverUpdateRequest request) {
        CaregiverProfile caregiverProfile = caregiverProfileRepository.findById(id)
                .orElseThrow(() -> new CaregiverNotFoundException(id));

        caregiverProfileMapper.updateEntityFromRequest(request, caregiverProfile);
        CaregiverProfile updatedProfile = caregiverProfileRepository.save(caregiverProfile);
        return caregiverProfileMapper.toResponse(updatedProfile);
    }

    /**
     * Updates an existing caregiver profile by user ID.
     * Only non-null fields from the request will be updated.
     *
     * @param userId  the user ID (typically from Keycloak)
     * @param request the update request containing fields to modify
     * @return the updated caregiver profile response
     * @throws CaregiverNotFoundException if no caregiver is found with the given user ID
     */
    @Transactional
    public CaregiverProfileResponse updateCaregiverByUserId(UUID userId, CaregiverUpdateRequest request) {
        CaregiverProfile caregiverProfile = caregiverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CaregiverNotFoundException("Caregiver profile not found with userId: " + userId));

        caregiverProfileMapper.updateEntityFromRequest(request, caregiverProfile);
        CaregiverProfile updatedProfile = caregiverProfileRepository.save(caregiverProfile);
        return caregiverProfileMapper.toResponse(updatedProfile);
    }
}
