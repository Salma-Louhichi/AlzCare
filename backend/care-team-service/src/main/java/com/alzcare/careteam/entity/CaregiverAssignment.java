package com.alzcare.careteam.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "caregiver_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaregiverAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String caregiverId;
    private String patientId;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private AssignmentStatus status;

    private LocalDateTime assignedAt;
    private LocalDateTime revokedAt;

    private String inviteToken;
    private LocalDateTime inviteExpiresAt;
    private LocalDateTime inviteAcceptedAt;

    private LocalDateTime unavailableFrom;
    private LocalDateTime unavailableTo;
    private String unavailabilityReason;

    public enum Role {
        PRIMARY, FAMILY, EMERGENCY
    }

    public enum AssignmentStatus {
        PENDING, ACTIVE, REVOKED
    }
}