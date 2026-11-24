package com.alzcare.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Security configuration for Spring Cloud Gateway.
 * 
 * Validates JWT tokens from Keycloak and extracts roles.
 * Public paths: /actuator/**, /realms/** (Keycloak endpoints via gateway)
 * Secured paths: All other routes require valid JWT
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${alzcare.cors.allowed-origin-patterns:http://localhost:4200,http://localhost:4201,http://127.0.0.1:4200,http://127.0.0.1:4201}")
    private List<String> allowedOriginPatterns;

    /**
     * Public paths that don't require authentication.
     */
    private static final String[] PUBLIC_PATHS = {
            "/actuator/**",
            "/actuator",
            "/realms/**",
            "/health",
            "/",
            "/favicon.ico"
    };

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(PUBLIC_PATHS).permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/care-team/invitations/*/validate").permitAll()
                        .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/v1/schedules/**").hasRole("ADMIN")
                        .pathMatchers("/api/v1/doctor/**").hasAnyRole("DOCTOR", "ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/v1/doctors/*/patients/create").hasAnyRole("DOCTOR", "ADMIN")
                        .pathMatchers("/api/v1/caregiver/**").hasAnyRole("CAREGIVER", "DOCTOR", "ADMIN")
                        .pathMatchers("/api/v1/patient/**").hasAnyRole("PATIENT", "CAREGIVER", "DOCTOR", "ADMIN")
                        // Event Ingestion Service - Camera management (Doctors/Admins)
                        .pathMatchers("/api/cameras").hasAnyRole("DOCTOR", "ADMIN")
                        .pathMatchers("/api/cameras/**").hasAnyRole("DOCTOR", "ADMIN")
                        // Event Ingestion Service - cameras post events with their device key, not a user JWT;
                        // event-ingestion authenticates the device itself (X-Device-Id / X-Device-Key)
                        .pathMatchers(HttpMethod.POST, "/api/events").permitAll()
                        // Event Ingestion Service - reading motion history and detected patterns
                        .pathMatchers("/api/events/**").hasAnyRole("DOCTOR", "ADMIN")
                        .pathMatchers("/api/patterns/**").hasAnyRole("DOCTOR", "ADMIN")
                        .pathMatchers("/api/behavior-logs/**").hasAnyRole("CAREGIVER", "DOCTOR", "ADMIN")
                        .pathMatchers("/api/alerts/**").hasAnyRole("PATIENT", "CAREGIVER", "DOCTOR", "ADMIN")
                        // Care Team Service - Admin/Doctor manage assignments, Caregivers participate
                        .pathMatchers("/api/v1/care-team/doctors/**").hasAnyRole("DOCTOR", "ADMIN")
                        .pathMatchers("/api/v1/care-team/caregivers/generate-invite")
                            .hasAnyRole("DOCTOR", "ADMIN", "CAREGIVER")
                        .pathMatchers("/api/v1/care-team/caregivers/assignments/**").hasAnyRole("CAREGIVER", "DOCTOR", "ADMIN")
                        .pathMatchers("/api/v1/care-team/caregivers/**").hasAnyRole("CAREGIVER", "DOCTOR", "ADMIN")
                        .pathMatchers("/api/v1/care-team/checklists/**").hasAnyRole("CAREGIVER", "DOCTOR", "ADMIN")
                        .pathMatchers("/api/v1/care-team/patients/**").hasAnyRole("PATIENT", "CAREGIVER", "DOCTOR", "ADMIN")
                        .pathMatchers("/api/v1/care-team/invitations/**").hasAnyRole("CAREGIVER", "DOCTOR", "ADMIN")
                        .pathMatchers("/api/v1/care-team/**").hasAnyRole("CAREGIVER", "DOCTOR", "ADMIN")
                        // Community Social Service - Forum discussions (open to all authenticated users)
                        .pathMatchers(HttpMethod.GET, "/community/api/v1/posts/**").authenticated()
                        .pathMatchers("/community/api/v1/posts/**").hasAnyRole("PATIENT", "CAREGIVER", "DOCTOR", "ADMIN")
                        // Community Social Service - Comments (open to all authenticated users)  
                        .pathMatchers(HttpMethod.GET, "/community/api/v1/comments/**").authenticated()
                        .pathMatchers("/community/api/v1/comments/**").hasAnyRole("PATIENT", "CAREGIVER", "DOCTOR", "ADMIN")
                        // Community Social Service - Activities (Admin/Doctor manage, others view)
                        .pathMatchers(HttpMethod.GET, "/community/api/v1/activities/**").authenticated()
                        .pathMatchers("/community/api/v1/activities/**").hasAnyRole("DOCTOR", "ADMIN")
                        // Community Social Service - Registrations (Patients register, others manage)
                        .pathMatchers(HttpMethod.GET, "/community/api/v1/registrations/**").authenticated()
                        .pathMatchers("/community/api/v1/registrations/**").hasAnyRole("PATIENT", "CAREGIVER", "DOCTOR", "ADMIN")
                        // ML Prediction Service - care-risk estimates are clinical information
                        .pathMatchers("/api/v1/ml/**").hasAnyRole("DOCTOR", "ADMIN")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(reactiveJwtAuthenticationConverter())))
                .build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        converter.setPrincipalClaimName(StandardClaimNames.PREFERRED_USERNAME);
        return converter;
    }

    private Flux<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Extract realm roles from the JWT
        List<String> roles = extractRoles(jwt);

        Collection<GrantedAuthority> authorities = roles.stream()
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return Flux.fromIterable(authorities);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        // Try to get from "roles" claim directly
        List<String> roles = jwt.getClaimAsStringList("roles");

        if (roles == null || roles.isEmpty()) {
            // Try to get from realm_access.roles
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                roles = (List<String>) realmAccess.get("roles");
            }
        }

        if (roles == null) {
            return List.of();
        }

        return roles;
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // setAllowedOrigins() does not understand wildcards (the former "http://*:4200" entry never
        // matched anything). Patterns are explicit and configurable per environment, e.g.
        // ALZCARE_CORS_ALLOWED_ORIGIN_PATTERNS=https://app.example.com
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
