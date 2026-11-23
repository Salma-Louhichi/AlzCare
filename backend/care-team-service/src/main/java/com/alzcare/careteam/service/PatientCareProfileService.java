package com.alzcare.careteam.service;

import com.alzcare.careteam.entity.PatientCareProfile;
import com.alzcare.careteam.repository.PatientCareProfileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PatientCareProfileService {

    private final PatientCareProfileRepository repo;

    public PatientCareProfileService(PatientCareProfileRepository repo) {
        this.repo = repo;
    }

    public PatientCareProfile upsert(String patientId, Map<String, String> body) {
        PatientCareProfile p = repo.findById(patientId).orElse(
                PatientCareProfile.builder().patientId(patientId).build()
        );
        if (body.containsKey("fullName")) {
            p.setFullName(nullIfBlank(body.get("fullName")));
        }
        if (body.containsKey("dateOfBirth")) {
            String dob = body.get("dateOfBirth");
            p.setDateOfBirth(dob == null || dob.isBlank() ? null : LocalDate.parse(dob));
        }
        if (body.containsKey("diagnosisStage")) {
            p.setDiagnosisStage(nullIfBlank(body.get("diagnosisStage")));
        }
        if (body.containsKey("address")) {
            p.setAddress(nullIfBlank(body.get("address")));
        }
        p.setUpdatedAt(LocalDateTime.now());
        return repo.save(p);
    }

    public PatientCareProfile get(String patientId) {
        return repo.findById(patientId)
                .orElseThrow(() -> new java.util.NoSuchElementException("No profile for patient " + patientId));
    }

    private static String nullIfBlank(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
