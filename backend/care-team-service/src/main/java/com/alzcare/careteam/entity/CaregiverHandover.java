package com.alzcare.careteam.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "caregiver_handovers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaregiverHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String patientId;
    private String fromCaregiverId;
    private String toCaregiverId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Boolean acknowledged;
    private LocalDateTime createdAt;
    private LocalDateTime acknowledgedAt;
}