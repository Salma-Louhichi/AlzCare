package com.alzcare.identity.services;

import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Lightweight Keycloak checks without depending on {@link KeycloakAdminService}
 * (avoids circular dependencies with profile services).
 */
@Service
@RequiredArgsConstructor
public class KeycloakRealmUserService {

    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.realm:alzcare}")
    private String realm;

    public boolean existsInRealm(String keycloakUserId) {
        try {
            keycloakAdminClient.realm(realm).users().get(keycloakUserId).toRepresentation();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
