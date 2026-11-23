package com.alzcare.careteam.dto;

public record InviteValidationResponse(
        boolean valid,
        String patientId,
        String role,
        String expiresAt
) {}
