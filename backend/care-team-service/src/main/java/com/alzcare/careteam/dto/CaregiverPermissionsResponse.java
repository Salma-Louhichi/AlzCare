package com.alzcare.careteam.dto;

import java.util.Map;

/**
 * FR2 — effective permissions for a caregiver on a given patient (API-level contract).
 */
public record CaregiverPermissionsResponse(
        String caregiverId,
        String patientId,
        String role,
        Map<String, Boolean> permissions
) {}
