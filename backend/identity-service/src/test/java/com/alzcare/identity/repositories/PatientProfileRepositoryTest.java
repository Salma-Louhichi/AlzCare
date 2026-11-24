package com.alzcare.identity.repositories;

import com.alzcare.identity.entities.PatientProfile;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PatientProfileRepository}.
 * Uses Testcontainers PostgreSQL for realistic database testing.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PatientProfile Repository Integration Tests")
class PatientProfileRepositoryTest {

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @BeforeEach
    void setUp() {
        patientProfileRepository.deleteAllInBatch();
    }

    private PatientProfile createPatientProfile(UUID userId, String firstName, String lastName, boolean isActive) {
        PatientProfile patient = new PatientProfile();
        patient.setUserId(userId);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setDateOfBirth(LocalDate.of(1950, 5, 15));
        patient.setGender(GenderEnum.MALE);
        patient.setPreferredLanguage(LanguageEnum.ENGLISH);
        patient.setIsActive(isActive);
        return patient;
    }

    @Test
    @DisplayName("Should save patient profile and generate UUID")
    void shouldSavePatientProfileAndGenerateUuid() {
        // Given
        PatientProfile patient = createPatientProfile(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"), "John", "Doe", true);

        // When
        PatientProfile saved = patientProfileRepository.saveAndFlush(patient);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find patient by Keycloak ID when exists")
    void shouldFindPatientByKeycloakIdWhenExists() {
        // Given
        PatientProfile saved = patientProfileRepository.saveAndFlush(createPatientProfile(UUID.fromString("550e8400-e29b-41d4-a716-446655440002"), "John", "Doe", true));

        // When
        Optional<PatientProfile> found = patientProfileRepository.findByUserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440002"));

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return empty optional when finding by non-existent Keycloak ID")
    void shouldReturnEmptyWhenFindingByNonExistentKeycloakId() {
        // When
        Optional<PatientProfile> found = patientProfileRepository.findByUserId(UUID.fromString("550e8400-e29b-41d4-a716-446655449999"));

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find all active patients")
    void shouldFindAllActivePatients() {
        // Given
        patientProfileRepository.saveAndFlush(createPatientProfile(UUID.fromString("550e8400-e29b-41d4-a716-446655440003"), "Active", "Patient", true));
        patientProfileRepository.saveAndFlush(createPatientProfile(UUID.fromString("550e8400-e29b-41d4-a716-446655440004"), "Inactive", "Patient", false));

        // When
        List<PatientProfile> activePatients = patientProfileRepository.findByIsActive(true);

        // Then
        assertThat(activePatients).hasSize(1);
        assertThat(activePatients.get(0).getUserId()).isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440003"));
    }

    @Test
    @DisplayName("Should find all inactive patients")
    void shouldFindAllInactivePatients() {
        // Given
        patientProfileRepository.saveAndFlush(createPatientProfile(UUID.fromString("550e8400-e29b-41d4-a716-446655440005"), "Active", "Patient", true));
        patientProfileRepository.saveAndFlush(createPatientProfile(UUID.fromString("550e8400-e29b-41d4-a716-446655440006"), "Inactive", "Patient", false));

        // When
        List<PatientProfile> inactivePatients = patientProfileRepository.findByIsActive(false);

        // Then
        assertThat(inactivePatients).hasSize(1);
        assertThat(inactivePatients.get(0).getUserId()).isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440006"));
    }

    @Test
    @DisplayName("Should update patient profile")
    void shouldUpdatePatientProfile() {
        // Given
        PatientProfile saved = patientProfileRepository.saveAndFlush(createPatientProfile(UUID.fromString("550e8400-e29b-41d4-a716-446655440007"), "Original", "Name", true));
        UUID id = saved.getId();

        // When
        saved.setFirstName("Updated");
        saved.setLastName("NameChanged");
        PatientProfile updated = patientProfileRepository.saveAndFlush(saved);

        // Then
        Optional<PatientProfile> found = patientProfileRepository.findById(id);
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Updated");
        assertThat(found.get().getLastName()).isEqualTo("NameChanged");
    }

    @Test
    @DisplayName("Should delete patient profile")
    void shouldDeletePatientProfile() {
        // Given
        PatientProfile saved = patientProfileRepository.saveAndFlush(createPatientProfile(UUID.fromString("550e8400-e29b-41d4-a716-446655440008"), "Delete", "Me", true));
        UUID id = saved.getId();

        // When
        patientProfileRepository.deleteById(id);

        // Then
        Optional<PatientProfile> found = patientProfileRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should verify unique constraint on keycloak_id")
    void shouldVerifyUniqueConstraintOnKeycloakId() {
        // Given
        patientProfileRepository.saveAndFlush(createPatientProfile(UUID.fromString("550e8400-e29b-41d4-a716-446655440009"), "First", "Patient", true));

        PatientProfile duplicate = new PatientProfile();
        duplicate.setUserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440009")); // Same user ID
        duplicate.setFirstName("Another");
        duplicate.setLastName("Person");
        duplicate.setDateOfBirth(LocalDate.of(1960, 1, 1));
        duplicate.setGender(GenderEnum.MALE);
        duplicate.setIsActive(true);

        // When/Then - Should throw exception due to unique constraint
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            patientProfileRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    @DisplayName("Should verify not null constraints on required fields")
    void shouldVerifyNotNullConstraints() {
        // Given - Patient without required fields
        PatientProfile invalidPatient = new PatientProfile();
        invalidPatient.setUserId(null); // null userId
        invalidPatient.setFirstName("Test");
        invalidPatient.setLastName("Test");
        invalidPatient.setDateOfBirth(LocalDate.of(1950, 5, 15));
        invalidPatient.setGender(GenderEnum.MALE);

        // When/Then - Should throw exception due to not null constraint
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            patientProfileRepository.saveAndFlush(invalidPatient);
        });
    }

    @Test
    @DisplayName("Should find patient by ID when exists")
    void shouldFindPatientByIdWhenExists() {
        // Given
        PatientProfile saved = patientProfileRepository.saveAndFlush(createPatientProfile(UUID.fromString("550e8400-e29b-41d4-a716-446655440010"), "ID", "Test", true));
        UUID id = saved.getId();

        // When
        Optional<PatientProfile> found = patientProfileRepository.findById(id);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440010"));
    }

    @Test
    @DisplayName("Should return empty when finding by non-existent ID")
    void shouldReturnEmptyWhenFindingByNonExistentId() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        Optional<PatientProfile> found = patientProfileRepository.findById(nonExistentId);

        // Then
        assertThat(found).isEmpty();
    }
}
