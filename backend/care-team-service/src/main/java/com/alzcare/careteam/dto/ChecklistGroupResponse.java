package com.alzcare.careteam.dto;

import com.alzcare.careteam.entity.ChecklistItem;

import java.time.LocalDate;
import java.util.List;

/** Logical checklist = items sharing (doctorId, patientId, date) per PDF module 3.2 */
public record ChecklistGroupResponse(
        String doctorId,
        String patientId,
        LocalDate date,
        String aggregateChecklistStatus,
        List<ChecklistItem> items
) {}
