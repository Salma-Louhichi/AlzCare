package com.alzcare.identity.dtos.requests;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String firstName;
    private String lastName;
    private Boolean enabled;
    private UserRole role;
    
    // Profile update fields
    private String dateOfBirth;
    private String gender;
    private String preferredLanguage;
    private String photoUrl;
    private String phone;
    private String emergencyContact;
    private String address;
    private String speciality;
    private String licenseNumber;
    private String contact;
    private String photoUrlCaregiver;
    private String phoneCaregiver;
    private String addressCaregiver;
    private String contactCaregiver;
    private Boolean isAvailable;
    private Boolean isProfessional;
    
    public enum UserRole {
        PATIENT, DOCTOR, CAREGIVER, ADMIN
    }
}
