package com.alzcare.careteam.dto;

import com.alzcare.careteam.entity.ChecklistItem;

import java.time.LocalDate;
import java.util.List;

public record GenerateChecklistRequest(
        String doctorId,
        String patientId,
        LocalDate date,
        List<ItemSeed> items
) {
    public record ItemSeed(
            String description,
            ChecklistItem.Category category,
            ChecklistItem.Priority priority,
            Integer itemOrder,
            String sourceAxisReference
    ) {}
}
