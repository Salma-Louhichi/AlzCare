package com.alzcare.identity.mappers;

import com.alzcare.identity.dtos.requests.DoctorCreateRequest;
import com.alzcare.identity.dtos.requests.DoctorUpdateRequest;
import com.alzcare.identity.dtos.responses.DoctorProfileResponse;
import com.alzcare.identity.entities.DoctorProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for DoctorProfile entity and DTOs.
 * Handles conversion between entity, request DTOs, and response DTOs.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface DoctorProfileMapper {

    /**
     * Converts DoctorProfile entity to DoctorProfileResponse DTO.
     *
     * @param doctorProfile the entity to convert
     * @return the response DTO
     */
    DoctorProfileResponse toResponse(DoctorProfile doctorProfile);

    /**
     * Converts DoctorProfileResponse DTO to DoctorProfile entity.
     * Note: This is primarily for testing purposes. 
     * Use builder pattern for creating new entities.
     *
     * @param response the response DTO
     * @return the entity
     */
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DoctorProfile toEntity(DoctorProfileResponse response);

    /**
     * Updates an existing DoctorProfile entity from DoctorUpdateRequest DTO.
     * Only non-null fields are updated.
     *
     * @param request the update request DTO
     * @param entity  the entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(DoctorUpdateRequest request, @MappingTarget DoctorProfile entity);

    /**
     * Converts DoctorCreateRequest DTO to DoctorProfile entity.
     *
     * @param request the create request DTO
     * @return the entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DoctorProfile toEntity(DoctorCreateRequest request);
}
