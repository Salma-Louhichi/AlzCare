package com.alzcare.careteam.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "checklist_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String doctorId;
    private String patientId;
    private LocalDate date;
    private Integer itemOrder;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private ItemStatus status;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String assignedCaregiverId;
    private String completedBy;
    private LocalDateTime completedAt;

    private String sourceAxisReference;
    private LocalDateTime checklistGeneratedAt;

    @Enumerated(EnumType.STRING)
    private ChecklistStatus checklistStatus;

    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    public enum ItemStatus {
        PENDING, ASSIGNED, COMPLETED
    }

    public enum Category {
        MEDICATION, INCIDENT, COGNITIVE_TEST, GENERAL
    }

    public enum ChecklistStatus {
        PENDING, COMPLETED
    }
}