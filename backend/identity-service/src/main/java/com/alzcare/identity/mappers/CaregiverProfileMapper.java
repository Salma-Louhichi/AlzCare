package com.alzcare.identity.mappers;

import com.alzcare.identity.dtos.requests.CaregiverCreateRequest;
import com.alzcare.identity.dtos.requests.CaregiverUpdateRequest;
import com.alzcare.identity.dtos.responses.CaregiverProfileResponse;
import com.alzcare.identity.entities.CaregiverProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for CaregiverProfile entity and DTOs.
 * Handles conversion between entity, request DTOs, and response DTOs.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CaregiverProfileMapper {

    /**
     * Converts CaregiverProfile entity to CaregiverProfileResponse DTO.
     *
     * @param caregiverProfile the entity to convert
     * @return the response DTO
     */
    CaregiverProfileResponse toResponse(CaregiverProfile caregiverProfile);

    /**
     * Converts CaregiverProfileResponse DTO to CaregiverProfile entity.
     * Note: This is primarily for testing purposes.
     * Use builder pattern for creating new entities.
     *
     * @param response the response DTO
     * @return the entity
     */
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CaregiverProfile toEntity(CaregiverProfileResponse response);

    /**
     * Updates an existing CaregiverProfile entity from CaregiverUpdateRequest DTO.
     * Only non-null fields are updated.
     *
     * @param request the update request DTO
     * @param entity  the entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(CaregiverUpdateRequest request, @MappingTarget CaregiverProfile entity);

    /**
     * Converts CaregiverCreateRequest DTO to CaregiverProfile entity.
     *
     * @param request the create request DTO
     * @return the entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CaregiverProfile toEntity(CaregiverCreateRequest request);
}
