package com.alzcare.identity.utils;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordGeneratorTest {

    @RepeatedTest(50)
    void satisfiesRealmPasswordPolicy() {
        String password = PasswordGenerator.generate();

        assertThat(password).hasSize(PasswordGenerator.DEFAULT_LENGTH);
        assertThat(password).containsPattern("[A-Z]");
        assertThat(password).containsPattern("[a-z]");
        assertThat(password).containsPattern("[0-9]");
        assertThat(password).containsPattern("[^A-Za-z0-9]");
    }

    @Test
    void avoidsLookAlikeCharacters() {
        for (int i = 0; i < 200; i++) {
            assertThat(PasswordGenerator.generate()).doesNotContainPattern("[0O1lI]");
        }
    }

    @Test
    void doesNotRepeatOrUsePredictablePrefix() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String password = PasswordGenerator.generate();
            assertThat(password).doesNotStartWith("Patient1!");
            assertThat(seen.add(password)).isTrue();
        }
    }

    @Test
    void rejectsTooShortLength() {
        assertThatThrownBy(() -> PasswordGenerator.generate(6))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
