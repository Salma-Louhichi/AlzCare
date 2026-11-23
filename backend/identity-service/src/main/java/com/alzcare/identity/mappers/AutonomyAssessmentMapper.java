package com.alzcare.identity.mappers;

import com.alzcare.identity.dtos.requests.AutonomyAssessmentRequest;
import com.alzcare.identity.dtos.responses.AutonomyAssessmentResponse;
import com.alzcare.identity.entities.AutonomyAssessment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for AutonomyAssessment entity and DTOs.
 * Handles conversion between entity, request DTOs, and response DTOs.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AutonomyAssessmentMapper {

    /**
     * Converts AutonomyAssessment entity to AutonomyAssessmentResponse DTO.
     *
     * @param autonomyAssessment the entity to convert
     * @return the response DTO
     */
    @Mapping(target = "patientId", source = "patient.id")
    AutonomyAssessmentResponse toResponse(AutonomyAssessment autonomyAssessment);

    /**
     * Converts AutonomyAssessmentRequest DTO to AutonomyAssessment entity.
     *
     * @param request the request DTO
     * @return the entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "overallScore", ignore = true)
    @Mapping(target = "patient", ignore = true)
    AutonomyAssessment toEntity(AutonomyAssessmentRequest request);
}
