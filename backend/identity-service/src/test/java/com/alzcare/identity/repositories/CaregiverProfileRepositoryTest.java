package com.alzcare.identity.repositories;

import com.alzcare.identity.entities.CaregiverProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CaregiverProfileRepository}.
 * Uses Testcontainers PostgreSQL for realistic database testing.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("CaregiverProfile Repository Integration Tests")
class CaregiverProfileRepositoryTest {

    @Autowired
    private CaregiverProfileRepository caregiverProfileRepository;

    @BeforeEach
    void setUp() {
        caregiverProfileRepository.deleteAllInBatch();
    }

    private CaregiverProfile createCaregiverProfile(UUID userId, String firstName, String lastName) {
        CaregiverProfile caregiver = new CaregiverProfile();
        caregiver.setUserId(userId);
        caregiver.setFirstName(firstName);
        caregiver.setLastName(lastName);
        caregiver.setPhone("+1234567890");
        caregiver.setAddress("123 Main St");
        caregiver.setContact("contact@example.com");
        caregiver.setIsAvailable(true);
        caregiver.setIsProfessional(false);
        return caregiver;
    }

    @Test
    @DisplayName("Should save caregiver profile and generate UUID")
    void shouldSaveCaregiverProfileAndGenerateUuid() {
        // Given
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        CaregiverProfile caregiver = createCaregiverProfile(userId, "Maria", "Garcia");

        // When
        CaregiverProfile saved = caregiverProfileRepository.saveAndFlush(caregiver);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getFirstName()).isEqualTo("Maria");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find caregiver by user ID when exists")
    void shouldFindCaregiverByUserIdWhenExists() {
        // Given
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        CaregiverProfile saved = caregiverProfileRepository.saveAndFlush(createCaregiverProfile(userId, "Test", "Caregiver"));

        // When
        Optional<CaregiverProfile> found = caregiverProfileRepository.findByUserId(userId);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getFirstName()).isEqualTo("Test");
    }

    @Test
    @DisplayName("Should return empty optional when finding by non-existent user ID")
    void shouldReturnEmptyWhenFindingByNonExistentUserId() {
        // When
        Optional<CaregiverProfile> found = caregiverProfileRepository.findByUserId(UUID.randomUUID());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should verify unique constraint on user_id")
    void shouldVerifyUniqueConstraintOnUserId() {
        // Given
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
        caregiverProfileRepository.saveAndFlush(createCaregiverProfile(userId, "First", "Caregiver"));
        
        CaregiverProfile duplicate = new CaregiverProfile();
        duplicate.setUserId(userId); // Same user ID
        duplicate.setFirstName("Another");
        duplicate.setLastName("Caregiver");
        duplicate.setIsAvailable(true);
        duplicate.setIsProfessional(false);

        // When/Then - Should throw exception due to unique constraint
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            caregiverProfileRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    @DisplayName("Should update caregiver profile")
    void shouldUpdateCaregiverProfile() {
        // Given
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440004");
        CaregiverProfile saved = caregiverProfileRepository.saveAndFlush(createCaregiverProfile(userId, "Original", "Name"));
        UUID id = saved.getId();

        // When
        saved.setPhone("+9998887777");
        saved.setAddress("456 New St");
        saved.setIsProfessional(true);
        CaregiverProfile updated = caregiverProfileRepository.saveAndFlush(saved);

        // Then
        Optional<CaregiverProfile> found = caregiverProfileRepository.findById(id);
        assertThat(found).isPresent();
        assertThat(found.get().getPhone()).isEqualTo("+9998887777");
        assertThat(found.get().getAddress()).isEqualTo("456 New St");
        assertThat(found.get().getIsProfessional()).isTrue();
    }

    @Test
    @DisplayName("Should delete caregiver profile")
    void shouldDeleteCaregiverProfile() {
        // Given
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440005");
        CaregiverProfile saved = caregiverProfileRepository.saveAndFlush(createCaregiverProfile(userId, "Delete", "Me"));
        UUID id = saved.getId();

        // When
        caregiverProfileRepository.deleteById(id);

        // Then
        Optional<CaregiverProfile> found = caregiverProfileRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should allow null optional fields")
    void shouldAllowNullOptionalFields() {
        // Given - Caregiver with minimal data
        CaregiverProfile minimalCaregiver = new CaregiverProfile();
        minimalCaregiver.setUserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440006"));
        minimalCaregiver.setFirstName("John");
        minimalCaregiver.setLastName("Doe");
        minimalCaregiver.setIsAvailable(true);
        minimalCaregiver.setIsProfessional(false);

        // When
        CaregiverProfile saved = caregiverProfileRepository.saveAndFlush(minimalCaregiver);

        // Then
        Optional<CaregiverProfile> found = caregiverProfileRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPhone()).isNull();
        assertThat(found.get().getAddress()).isNull();
        assertThat(found.get().getContact()).isNull();
        assertThat(found.get().getPhotoUrl()).isNull();
    }

    @Test
    @DisplayName("Should find caregiver by ID when exists")
    void shouldFindCaregiverByIdWhenExists() {
        // Given
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440007");
        CaregiverProfile saved = caregiverProfileRepository.saveAndFlush(createCaregiverProfile(userId, "ID", "Test"));
        UUID id = saved.getId();

        // When
        Optional<CaregiverProfile> found = caregiverProfileRepository.findById(id);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should return empty when finding by non-existent ID")
    void shouldReturnEmptyWhenFindingByNonExistentId() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        Optional<CaregiverProfile> found = caregiverProfileRepository.findById(nonExistentId);

        // Then
        assertThat(found).isEmpty();
    }
}
