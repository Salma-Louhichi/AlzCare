package com.alzcare.careteam.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CaregiverAvailabilityResponse(
        UUID assignmentId,
        String patientId,
        LocalDateTime unavailableFrom,
        LocalDateTime unavailableTo,
        String reason
) {}
