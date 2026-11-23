package com.alzcare.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Identity Service - Main Application Entry Point
 * 
 * <p>Manages patient, doctor, and caregiver profiles along with
 * autonomy assessments for the AlzCare platform.</p>
 * 
 * <p>Port: 8001</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }

}
