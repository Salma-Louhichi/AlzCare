package com.alzcare.identity.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {

    @Value("${keycloak.auth-server-url:http://localhost:8090}")
    private String serverUrl;

    @Value("${keycloak.realm:alzcare}")
    private String realm;

    @Value("${keycloak.admin-client-id:admin-cli}")
    private String clientId;

    @Value("${keycloak.admin-username:admin}")
    private String username;

    @Value("${keycloak.admin-password:admin}")
    private String password;

    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")  // Connect to master realm for admin operations
                .clientId(clientId)
                .username(username)
                .password(password)
                .build();
    }
}
