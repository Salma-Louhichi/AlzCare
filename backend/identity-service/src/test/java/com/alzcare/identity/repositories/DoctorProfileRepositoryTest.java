package com.alzcare.identity.repositories;

import com.alzcare.identity.entities.DoctorProfile;
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
 * Integration tests for {@link DoctorProfileRepository}.
 * Uses Testcontainers PostgreSQL for realistic database testing.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("DoctorProfile Repository Integration Tests")
class DoctorProfileRepositoryTest {

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @BeforeEach
    void setUp() {
        doctorProfileRepository.deleteAll();
    }

    private DoctorProfile createDoctorProfile(UUID userId, String firstName, String lastName, 
                                               String speciality, String licenseNumber) {
        DoctorProfile doctor = new DoctorProfile();
        doctor.setUserId(userId);
        doctor.setFirstName(firstName);
        doctor.setLastName(lastName);
        doctor.setSpeciality(speciality);
        doctor.setLicenseNumber(licenseNumber);
        doctor.setIsAvailable(true);
        return doctor;
    }

    @Test
    @DisplayName("Should save doctor profile and generate UUID")
    void shouldSaveDoctorProfileAndGenerateUuid() {
        // Given
        UUID userId = UUID.randomUUID();
        DoctorProfile doctor = createDoctorProfile(userId, "Dr. Sarah", "Johnson", "Neurology", "MD-NEURO-789");

        // When
        DoctorProfile saved = doctorProfileRepository.save(doctor);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getFirstName()).isEqualTo("Dr. Sarah");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find doctor by user ID when exists")
    void shouldFindDoctorByUserIdWhenExists() {
        // Given
        UUID userId = UUID.randomUUID();
        DoctorProfile saved = doctorProfileRepository.save(createDoctorProfile(userId, "Dr. Test", "Doctor", "Cardiology", "MD-001"));

        // When
        Optional<DoctorProfile> found = doctorProfileRepository.findByUserId(userId);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getFirstName()).isEqualTo("Dr. Test");
    }

    @Test
    @DisplayName("Should return empty optional when finding by non-existent user ID")
    void shouldReturnEmptyWhenFindingByNonExistentUserId() {
        // When
        Optional<DoctorProfile> found = doctorProfileRepository.findByUserId(UUID.randomUUID());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find doctor by license number when exists")
    void shouldFindDoctorByLicenseNumberWhenExists() {
        // Given
        UUID userId = UUID.randomUUID();
        DoctorProfile saved = doctorProfileRepository.save(createDoctorProfile(userId, "Dr. Test", "Doctor", "Neurology", "LICENSE-123"));

        // When
        Optional<DoctorProfile> found = doctorProfileRepository.findByLicenseNumber("LICENSE-123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getLicenseNumber()).isEqualTo("LICENSE-123");
    }

    @Test
    @DisplayName("Should return empty optional when finding by non-existent license number")
    void shouldReturnEmptyWhenFindingByNonExistentLicenseNumber() {
        // When
        Optional<DoctorProfile> found = doctorProfileRepository.findByLicenseNumber("NON-EXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should verify unique constraint on user_id")
    void shouldVerifyUniqueConstraintOnUserId() {
        // Given
        UUID duplicateUserId = UUID.randomUUID();
        doctorProfileRepository.save(createDoctorProfile(duplicateUserId, "Dr. First", "Doctor", "Cardiology", "MD-001"));
        
        DoctorProfile duplicate = new DoctorProfile();
        duplicate.setUserId(duplicateUserId); // Same user ID
        duplicate.setFirstName("Another");
        duplicate.setLastName("Doctor");
        duplicate.setSpeciality("Neurology");
        duplicate.setLicenseNumber("MD-002");
        duplicate.setIsAvailable(true);

        // When/Then - Should throw exception due to unique constraint
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            doctorProfileRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    @DisplayName("Should verify unique constraint on license_number")
    void shouldVerifyUniqueConstraintOnLicenseNumber() {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        doctorProfileRepository.save(createDoctorProfile(userId1, "Dr. First", "Doctor", "Cardiology", "UNIQUE-LICENSE"));
        
        DoctorProfile duplicateLicense = new DoctorProfile();
        duplicateLicense.setUserId(userId2);
        duplicateLicense.setFirstName("Another");
        duplicateLicense.setLastName("Doctor");
        duplicateLicense.setSpeciality("Neurology");
        duplicateLicense.setLicenseNumber("UNIQUE-LICENSE"); // Same license number
        duplicateLicense.setIsAvailable(true);

        // When/Then - Should throw exception due to unique constraint
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            doctorProfileRepository.saveAndFlush(duplicateLicense);
        });
    }

    @Test
    @DisplayName("Should update doctor profile")
    void shouldUpdateDoctorProfile() {
        // Given
        UUID userId = UUID.randomUUID();
        DoctorProfile saved = doctorProfileRepository.save(createDoctorProfile(userId, "Dr. Original", "Name", "Cardiology", "MD-UPD"));
        UUID id = saved.getId();

        // When
        saved.setSpeciality("Geriatrics");
        saved.setLicenseNumber("MD-GERI-123");
        DoctorProfile updated = doctorProfileRepository.save(saved);

        // Then
        Optional<DoctorProfile> found = doctorProfileRepository.findById(id);
        assertThat(found).isPresent();
        assertThat(found.get().getSpeciality()).isEqualTo("Geriatrics");
        assertThat(found.get().getLicenseNumber()).isEqualTo("MD-GERI-123");
    }

    @Test
    @DisplayName("Should delete doctor profile")
    void shouldDeleteDoctorProfile() {
        // Given
        UUID userId = UUID.randomUUID();
        DoctorProfile saved = doctorProfileRepository.save(createDoctorProfile(userId, "Dr. Delete", "Me", "Cardiology", "MD-DEL"));
        UUID id = saved.getId();

        // When
        doctorProfileRepository.deleteById(id);

        // Then
        Optional<DoctorProfile> found = doctorProfileRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should store and retrieve new contact fields")
    void shouldStoreAndRetrieveNewContactFields() {
        // Given
        UUID userId = UUID.randomUUID();
        DoctorProfile doctor = createDoctorProfile(userId, "Dr. Contact", "Test", "Cardiology", "MD-CONTACT");
        doctor.setPhotoUrl("https://example.com/photo.jpg");
        doctor.setPhone("+1-555-123-4567");
        doctor.setContact("doctor@example.com");
        doctor.setAddress("123 Medical Center Drive, Suite 100");
        
        DoctorProfile saved = doctorProfileRepository.save(doctor);

        // When
        Optional<DoctorProfile> found = doctorProfileRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getPhotoUrl()).isEqualTo("https://example.com/photo.jpg");
        assertThat(found.get().getPhone()).isEqualTo("+1-555-123-4567");
        assertThat(found.get().getContact()).isEqualTo("doctor@example.com");
        assertThat(found.get().getAddress()).isEqualTo("123 Medical Center Drive, Suite 100");
    }

    @Test
    @DisplayName("Should store and retrieve isAvailable field")
    void shouldStoreAndRetrieveIsAvailableField() {
        // Given
        UUID userId = UUID.randomUUID();
        DoctorProfile doctor = createDoctorProfile(userId, "Dr. Available", "Test", "Neurology", "MD-AVAIL");
        doctor.setIsAvailable(false);
        
        DoctorProfile saved = doctorProfileRepository.save(doctor);

        // When
        Optional<DoctorProfile> found = doctorProfileRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getIsAvailable()).isFalse();
    }
}
