package com.alzcare.careteam.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class JwtRealmRoles {

    private JwtRealmRoles() {
    }

    @SuppressWarnings("unchecked")
    public static List<String> extract(Jwt jwt) {
        if (jwt == null) {
            return List.of();
        }
        List<String> direct = jwt.getClaimAsStringList("roles");
        if (direct != null && !direct.isEmpty()) {
            return direct;
        }
        // Some tokens expose realm roles only as a single JSON string or under realm_access
        Object rolesClaim = jwt.getClaim("roles");
        if (rolesClaim instanceof String s && !s.isBlank()) {
            return List.of(s.trim());
        }
        Map<String, Object> realm = jwt.getClaim("realm_access");
        if (realm == null) {
            return List.of();
        }
        Object r = realm.get("roles");
        if (r instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return Collections.emptyList();
    }
}
