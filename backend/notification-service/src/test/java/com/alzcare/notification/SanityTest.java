package com.alzcare.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Simple sanity test to verify that the Spring context loads correctly.
 */
@SpringBootTest
@ActiveProfiles("dev")
public class SanityTest {

    @Test
    void contextLoads() {
        // Just verify Spring context loads
    }
}
