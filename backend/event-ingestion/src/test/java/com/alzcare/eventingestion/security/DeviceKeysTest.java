package com.alzcare.eventingestion.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceKeysTest {

    @Test
    void keysAreLongRandomAndUrlSafe() {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String key = DeviceKeys.generate();
            assertThat(key).hasSize(43).matches("[A-Za-z0-9_-]+");
            keys.add(key);
        }
        assertThat(keys).hasSize(500);
    }

    @Test
    void onlyTheHashIsComparable() {
        String key = DeviceKeys.generate();
        String hash = DeviceKeys.hash(key);
        assertThat(hash).hasSize(64).doesNotContain(key);
        assertThat(DeviceKeys.matches(key, hash)).isTrue();
        assertThat(DeviceKeys.matches(key + "x", hash)).isFalse();
        assertThat(DeviceKeys.matches(null, hash)).isFalse();
    }
}
