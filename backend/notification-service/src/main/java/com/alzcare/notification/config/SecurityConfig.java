package com.alzcare.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the notification service.
 * <p>Configures stateless authentication. The gateway handles JWT validation,
 * so this service trusts requests that come through the gateway.</p>
 * <p>Enables method-level security for @PreAuthorize annotations.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless REST API
            .csrf(csrf -> csrf.disable())
            
            // Stateless session management (no cookies)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Authorization rules - permit all since gateway handles auth
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            
            // Disable form login (prevents redirect to /login)
            .formLogin(form -> form.disable())
            
            // Disable HTTP Basic
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
