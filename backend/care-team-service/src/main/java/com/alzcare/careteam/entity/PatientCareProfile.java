package com.alzcare.careteam.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Care-coordination view of patient demographics (Axis 3). Identity service may hold the canonical user;
 * this supports PUT /patients/{id} from the PDF without coupling to other microservices in the demo.
 */
@Entity
@Table(name = "patient_care_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientCareProfile {

    @Id
    @Column(length = 128)
    private String patientId;

    private String fullName;

    private LocalDate dateOfBirth;

    private String diagnosisStage;

    @Column(columnDefinition = "TEXT")
    private String address;

    private LocalDateTime updatedAt;
}
