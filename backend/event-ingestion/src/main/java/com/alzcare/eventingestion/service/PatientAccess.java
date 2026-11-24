package com.alzcare.eventingestion.service;

import com.alzcare.eventingestion.client.CareTeamClient;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/** A doctor may only see or manage the home monitoring of their own patients; admins see all. */
@Component
public class PatientAccess {

    private final CareTeamClient careTeam;

    public PatientAccess(CareTeamClient careTeam) {
        this.careTeam = careTeam;
    }

    public void check(Jwt jwt, String patientId) {
        if (isAdmin(jwt)) {
            return;
        }
        String doctorId = careTeam.activeDoctorId(patientId).orElse(null);
        if (!jwt.getSubject().equals(doctorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Patient is not assigned to you");
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean isAdmin(Jwt jwt) {
        Object realm = jwt.getClaimAsMap("realm_access") == null ? null : jwt.getClaimAsMap("realm_access").get("roles");
        List<String> roles = jwt.getClaimAsStringList("roles");
        return (roles != null && roles.contains("ADMIN"))
                || (realm instanceof List<?> list && ((List<Object>) list).contains("ADMIN"));
    }
}
