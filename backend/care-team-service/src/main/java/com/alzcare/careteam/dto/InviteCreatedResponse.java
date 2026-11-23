package com.alzcare.careteam.dto;

import java.util.UUID;

public record InviteCreatedResponse(
        UUID assignmentId,
        String inviteToken,
        String inviteUrl,
        String expiresAt,
        String patientId,
        String role
) {}
