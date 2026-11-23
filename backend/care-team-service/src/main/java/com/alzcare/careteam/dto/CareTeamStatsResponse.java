package com.alzcare.careteam.dto;

/** Matches Angular CareTeamStats for admin / demo dashboards (Sprint 1 AA1). */
public record CareTeamStatsResponse(
        int totalCaregivers,
        int totalDoctors,
        int activeAssignments,
        int pendingInvites
) {}
