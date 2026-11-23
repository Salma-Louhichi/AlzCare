package com.alzcare.identity.mappers;

import com.alzcare.identity.dtos.requests.PatientCreateRequest;
import com.alzcare.identity.dtos.requests.PatientUpdateRequest;
import com.alzcare.identity.dtos.responses.PatientProfileResponse;
import com.alzcare.identity.entities.PatientProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for PatientProfile entity and DTOs.
 * Handles conversion between entity, request DTOs, and response DTOs.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PatientProfileMapper {

    /**
     * Converts PatientProfile entity to PatientProfileResponse DTO.
     *
     * @param patientProfile the entity to convert
     * @return the response DTO
     */
    PatientProfileResponse toResponse(PatientProfile patientProfile);

    /**
     * Converts PatientCreateRequest DTO to PatientProfile entity.
     * Sets default preferred language to ENGLISH if not provided.
     *
     * @param request the create request DTO
     * @return the entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "autonomyAssessments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PatientProfile toEntity(PatientCreateRequest request);

    /**
     * Updates an existing PatientProfile entity from PatientUpdateRequest DTO.
     * Only non-null fields are updated.
     *
     * @param request the update request DTO
     * @param entity  the entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "autonomyAssessments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(PatientUpdateRequest request, @MappingTarget PatientProfile entity);
}
