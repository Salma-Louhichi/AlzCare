package com.alzcare.careteam.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "doctor_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String doctorId;
    private String patientId;
    private String createdBy;

    @Enumerated(EnumType.STRING)
    private DoctorAssignmentStatus status;

    private LocalDateTime assignedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum DoctorAssignmentStatus {
        ACTIVE, INACTIVE
    }
}