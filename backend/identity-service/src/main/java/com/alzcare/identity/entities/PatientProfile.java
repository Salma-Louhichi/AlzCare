package com.alzcare.identity.entities;

import com.alzcare.identity.enums.GenderEnum;
import com.alzcare.identity.enums.LanguageEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core patient demographic and preference data.
 * Linked to Keycloak for authentication.
 */
@Entity
@Table(name = "patient_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "gender_enum")
    private GenderEnum gender;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "preferred_language", columnDefinition = "language_enum")
    @Builder.Default
    private LanguageEnum preferredLanguage = LanguageEnum.ENGLISH;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "emergency_contact", length = 255)
    private String emergencyContact;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "total_points")
    private Integer totalPoints;

    @Column(name = "current_streak")
    private Integer currentStreak;

    @Column(name = "last_played_date")
    private LocalDate lastPlayedDate;

    @Column(name = "assisted_mode_active")
    private Boolean assistedModeActive;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AutonomyAssessment> autonomyAssessments = new ArrayList<>();

    /**
     * Helper method to add an autonomy assessment.
     *
     * @param assessment the assessment to add
     */
    public void addAutonomyAssessment(AutonomyAssessment assessment) {
        autonomyAssessments.add(assessment);
    }

    /**
     * Helper method to remove an autonomy assessment.
     *
     * @param assessment the assessment to remove
     */
    public void removeAutonomyAssessment(AutonomyAssessment assessment) {
        autonomyAssessments.remove(assessment);
    }
}
