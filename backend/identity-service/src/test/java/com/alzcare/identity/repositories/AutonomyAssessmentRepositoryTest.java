package com.alzcare.identity.repositories;

import com.alzcare.identity.entities.AutonomyAssessment;
import com.alzcare.identity.entities.PatientProfile;
import com.alzcare.identity.enums.AutonomyLevel;
import com.alzcare.identity.enums.GenderEnum;
import com.alzcare.identity.enums.LanguageEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AutonomyAssessmentRepository}.
 * Uses Testcontainers PostgreSQL for realistic database testing.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AutonomyAssessment Repository Integration Tests")
class AutonomyAssessmentRepositoryTest {

    @Autowired
    private AutonomyAssessmentRepository autonomyAssessmentRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    private PatientProfile patient;

    @BeforeEach
    void setUp() {
        autonomyAssessmentRepository.deleteAll();
        patientProfileRepository.deleteAll();

        patient = new PatientProfile();
        patient.setUserId(UUID.nameUUIDFromBytes("patient-assessment-123".getBytes()));
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setDateOfBirth(LocalDate.of(1950, 5, 15));
        patient.setGender(GenderEnum.MALE);
        patient.setPreferredLanguage(LanguageEnum.ENGLISH);
        patient.setIsActive(true);
        patient = patientProfileRepository.save(patient);
    }

    private AutonomyAssessment createAssessment(AutonomyLevel hygiene, AutonomyLevel medication, 
                                                 AutonomyLevel mobility, AutonomyLevel feeding,
                                                 String assessedBy, OffsetDateTime assessedAt) {
        AutonomyAssessment assessment = new AutonomyAssessment();
        assessment.setPatient(patient);
        assessment.setHygieneLevel(hygiene);
        assessment.setMedicationLevel(medication);
        assessment.setMobilityLevel(mobility);
        assessment.setFeedingLevel(feeding);
        assessment.setAssessedBy(UUID.nameUUIDFromBytes(assessedBy.getBytes()));
        assessment.setAssessmentDate((assessedAt).toLocalDate());
        return assessment;
    }

    @Test
    @DisplayName("Should save autonomy assessment and generate UUID")
    void shouldSaveAutonomyAssessmentAndGenerateUuid() {
        // Given
        AutonomyAssessment assessment = createAssessment(
                AutonomyLevel.ASSISTED, AutonomyLevel.INDEPENDENT, 
                AutonomyLevel.ASSISTED, AutonomyLevel.INDEPENDENT,
                "Dr. Smith", OffsetDateTime.now()
        );
        assessment.setNotes("Patient shows good progress");

        // When
        AutonomyAssessment saved = autonomyAssessmentRepository.save(assessment);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getOverallScore()).isNotNull();
    }

    @Test
    @DisplayName("Should calculate overall score correctly")
    void shouldCalculateOverallScoreCorrectly() {
        // Given - INDEPENDENT(3) + ASSISTED(2) + DEPENDENT(1) + INDEPENDENT(3) = 9
        // Score = (9/12) * 100 = 75%
        AutonomyAssessment assessment = createAssessment(
                AutonomyLevel.INDEPENDENT, AutonomyLevel.ASSISTED,
                AutonomyLevel.DEPENDENT, AutonomyLevel.INDEPENDENT,
                "Dr. Smith", OffsetDateTime.now()
        );

        // When
        AutonomyAssessment saved = autonomyAssessmentRepository.save(assessment);

        // Then
        assertThat(saved.getOverallScore()).isEqualTo(75);
    }

    @Test
    @DisplayName("Should calculate maximum score for all independent")
    void shouldCalculateMaximumScoreForAllIndependent() {
        // Given - All INDEPENDENT = 12 points = 100%
        AutonomyAssessment assessment = createAssessment(
                AutonomyLevel.INDEPENDENT, AutonomyLevel.INDEPENDENT,
                AutonomyLevel.INDEPENDENT, AutonomyLevel.INDEPENDENT,
                "Dr. Smith", OffsetDateTime.now()
        );

        // When
        AutonomyAssessment saved = autonomyAssessmentRepository.save(assessment);

        // Then
        assertThat(saved.getOverallScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should calculate minimum score for all dependent")
    void shouldCalculateMinimumScoreForAllDependent() {
        // Given - All DEPENDENT = 4 points = 33%
        AutonomyAssessment assessment = createAssessment(
                AutonomyLevel.DEPENDENT, AutonomyLevel.DEPENDENT,
                AutonomyLevel.DEPENDENT, AutonomyLevel.DEPENDENT,
                "Dr. Smith", OffsetDateTime.now()
        );

        // When
        AutonomyAssessment saved = autonomyAssessmentRepository.save(assessment);

        // Then
        assertThat(saved.getOverallScore()).isEqualTo(33);
    }

    @Test
    @DisplayName("Should find all assessments by patient ID")
    void shouldFindAllAssessmentsByPatientId() {
        // Given
        AutonomyAssessment assessment1 = createAssessment(
                AutonomyLevel.ASSISTED, AutonomyLevel.INDEPENDENT,
                AutonomyLevel.ASSISTED, AutonomyLevel.INDEPENDENT,
                "Dr. Smith", OffsetDateTime.now().minusDays(7)
        );

        AutonomyAssessment assessment2 = createAssessment(
                AutonomyLevel.INDEPENDENT, AutonomyLevel.INDEPENDENT,
                AutonomyLevel.ASSISTED, AutonomyLevel.INDEPENDENT,
                "Dr. Johnson", OffsetDateTime.now()
        );

        autonomyAssessmentRepository.save(assessment1);
        autonomyAssessmentRepository.save(assessment2);

        // When
        List<AutonomyAssessment> assessments = autonomyAssessmentRepository.findByPatient_Id(patient.getId());

        // Then
        assertThat(assessments).hasSize(2);
    }

    @Test
    @DisplayName("Should return empty list when no assessments for patient")
    void shouldReturnEmptyListWhenNoAssessmentsForPatient() {
        // Given - no assessments saved
        UUID patientId = patient.getId();

        // When
        List<AutonomyAssessment> assessments = autonomyAssessmentRepository.findByPatient_Id(patientId);

        // Then
        assertThat(assessments).isEmpty();
    }

    @Test
    @DisplayName("Should find latest assessment by patient ID")
    void shouldFindLatestAssessmentByPatientId() {
        // Given
        AutonomyAssessment oldAssessment = createAssessment(
                AutonomyLevel.DEPENDENT, AutonomyLevel.DEPENDENT,
                AutonomyLevel.DEPENDENT, AutonomyLevel.DEPENDENT,
                "Dr. Old", OffsetDateTime.now().minusDays(30)
        );

        AutonomyAssessment recentAssessment = createAssessment(
                AutonomyLevel.INDEPENDENT, AutonomyLevel.INDEPENDENT,
                AutonomyLevel.INDEPENDENT, AutonomyLevel.INDEPENDENT,
                "Dr. Recent", OffsetDateTime.now()
        );

        autonomyAssessmentRepository.save(oldAssessment);
        autonomyAssessmentRepository.save(recentAssessment);

        // When
        Optional<AutonomyAssessment> latest = autonomyAssessmentRepository
                .findTopByPatient_IdOrderByCreatedAtDesc(patient.getId());

        // Then
        assertThat(latest).isPresent();
        assertThat(latest.get().getAssessedBy()).isEqualTo(UUID.nameUUIDFromBytes("Dr. Recent".getBytes()));
        assertThat(latest.get().getOverallScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should return empty optional when no latest assessment exists")
    void shouldReturnEmptyWhenNoLatestAssessmentExists() {
        // Given - no assessments saved

        // When
        Optional<AutonomyAssessment> latest = autonomyAssessmentRepository
                .findTopByPatient_IdOrderByCreatedAtDesc(patient.getId());

        // Then
        assertThat(latest).isEmpty();
    }

    @Test
    @DisplayName("Should verify foreign key constraint on patient_id")
    void shouldVerifyForeignKeyConstraintOnPatientId() {
        // Given - Assessment with non-existent patient
        PatientProfile nonExistentPatient = new PatientProfile();
        nonExistentPatient.setId(UUID.randomUUID());
        nonExistentPatient.setUserId(UUID.nameUUIDFromBytes("non-existent".getBytes()));
        nonExistentPatient.setFirstName("Non");
        nonExistentPatient.setLastName("Existent");
        nonExistentPatient.setDateOfBirth(LocalDate.of(1950, 1, 1));
        nonExistentPatient.setGender(GenderEnum.MALE);

        AutonomyAssessment assessment = createAssessment(
                AutonomyLevel.ASSISTED, AutonomyLevel.ASSISTED,
                AutonomyLevel.ASSISTED, AutonomyLevel.ASSISTED,
                "Dr. Smith", OffsetDateTime.now()
        );
        assessment.setPatient(nonExistentPatient);

        // When/Then - Should throw exception due to FK constraint
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            autonomyAssessmentRepository.saveAndFlush(assessment);
        });
    }

    @Test
    @DisplayName("Should delete assessment when patient is deleted")
    void shouldDeleteAssessmentWhenPatientIsDeleted() {
        // Given
        AutonomyAssessment assessment = createAssessment(
                AutonomyLevel.ASSISTED, AutonomyLevel.ASSISTED,
                AutonomyLevel.ASSISTED, AutonomyLevel.ASSISTED,
                "Dr. Smith", OffsetDateTime.now()
        );
        AutonomyAssessment saved = autonomyAssessmentRepository.save(assessment);
        UUID assessmentId = saved.getId();

        // When
        patientProfileRepository.deleteById(patient.getId());

        // Then
        Optional<AutonomyAssessment> found = autonomyAssessmentRepository.findById(assessmentId);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should update assessment")
    void shouldUpdateAssessment() {
        // Given
        AutonomyAssessment assessment = createAssessment(
                AutonomyLevel.ASSISTED, AutonomyLevel.ASSISTED,
                AutonomyLevel.ASSISTED, AutonomyLevel.ASSISTED,
                "Dr. Smith", OffsetDateTime.now()
        );
        assessment.setNotes("Initial assessment");
        AutonomyAssessment saved = autonomyAssessmentRepository.save(assessment);
        UUID id = saved.getId();

        // When
        saved.setHygieneLevel(AutonomyLevel.INDEPENDENT);
        saved.setNotes("Updated assessment - patient improved");
        AutonomyAssessment updated = autonomyAssessmentRepository.save(saved);

        // Then
        Optional<AutonomyAssessment> found = autonomyAssessmentRepository.findById(id);
        assertThat(found).isPresent();
        assertThat(found.get().getHygieneLevel()).isEqualTo(AutonomyLevel.INDEPENDENT);
        assertThat(found.get().getNotes()).isEqualTo("Updated assessment - patient improved");
        // Score should be recalculated
        assertThat(found.get().getOverallScore()).isGreaterThan(saved.getOverallScore());
    }

    @Test
    @DisplayName("Should store and retrieve notes field")
    void shouldStoreAndRetrieveNotesField() {
        // Given
        String longNotes = "Patient shows significant improvement in daily activities. " +
                "Hygiene: Can now brush teeth independently with reminders. " +
                "Medication: Takes all medications on time. " +
                "Mobility: Still needs assistance with stairs. " +
                "Feeding: Eats independently.";

        AutonomyAssessment assessment = createAssessment(
                AutonomyLevel.ASSISTED, AutonomyLevel.INDEPENDENT,
                AutonomyLevel.ASSISTED, AutonomyLevel.INDEPENDENT,
                "Dr. Smith", OffsetDateTime.now()
        );
        assessment.setNotes(longNotes);

        // When
        AutonomyAssessment saved = autonomyAssessmentRepository.save(assessment);

        // Then
        Optional<AutonomyAssessment> found = autonomyAssessmentRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getNotes()).isEqualTo(longNotes);
    }
}
