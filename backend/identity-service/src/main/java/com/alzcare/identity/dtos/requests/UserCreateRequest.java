package com.alzcare.identity.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class UserCreateRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @NotNull(message = "Role is required")
    private UserRole role;
    
    @Builder.Default
    private Boolean emailVerified = false;
    @Builder.Default
    private Boolean enabled = true;
    
    // Patient-specific fields
    private String dateOfBirth;
    private String gender;
    private String preferredLanguage;
    private String photoUrl;
    private String phone;
    private String emergencyContact;
    private String address;
    
    // Doctor-specific fields
    private String speciality;
    private String licenseNumber;
    private String photoUrlDoctor;
    private String phoneDoctor;
    private String contact;
    private String addressDoctor;
    private Boolean isAvailable;
    
    // Caregiver-specific fields
    private String photoUrlCaregiver;
    private String phoneCaregiver;
    private String addressCaregiver;
    private String contactCaregiver;
    private Boolean isAvailableCaregiver;
    private Boolean isProfessional;
    
    public enum UserRole {
        PATIENT, DOCTOR, CAREGIVER, ADMIN
    }
}
