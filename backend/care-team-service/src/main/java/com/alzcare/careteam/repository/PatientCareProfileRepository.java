package com.alzcare.careteam.repository;

import com.alzcare.careteam.entity.PatientCareProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientCareProfileRepository extends JpaRepository<PatientCareProfile, String> {
}
