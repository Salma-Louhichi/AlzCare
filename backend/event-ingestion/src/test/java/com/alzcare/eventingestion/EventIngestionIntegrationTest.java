package com.alzcare.eventingestion;

import com.alzcare.eventingestion.client.CareTeamClient;
import com.alzcare.eventingestion.domain.CameraStatus;
import com.alzcare.eventingestion.domain.PatternType;
import com.alzcare.eventingestion.messaging.NotificationEvent;
import com.alzcare.eventingestion.repository.ActivityEventRepository;
import com.alzcare.eventingestion.repository.CameraDeviceRepository;
import com.alzcare.eventingestion.repository.DetectedPatternRepository;
import com.alzcare.eventingestion.service.CameraHealthMonitor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end through HTTP, both security chains and a real PostgreSQL (Flyway schema).
 * The clock is fixed at 13:00 Tunis time; care-team, RabbitMQ and Keycloak are mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class EventIngestionIntegrationTest {

    static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");
    static final Instant NOW = LocalDateTime.of(2026, 11, 24, 13, 0).atZone(TUNIS).toInstant();
    static final String DOCTOR = "d0000000-0000-4000-8000-000000000002";
    static final String CAREGIVER = "c0000000-0000-4000-8000-000000000003";
    static final String PATIENT = "e30b2ed0-dd28-4cf8-b338-9c69a9a71353";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CameraDeviceRepository cameras;
    @Autowired ActivityEventRepository events;
    @Autowired DetectedPatternRepository patterns;
    @Autowired CameraHealthMonitor healthMonitor;

    @MockBean CareTeamClient careTeam;
    @MockBean RabbitTemplate rabbit;
    @MockBean JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        patterns.deleteAll();
        events.deleteAll();
        cameras.deleteAll();
        when(careTeam.activeDoctorId(PATIENT)).thenReturn(Optional.of(DOCTOR));
        when(careTeam.activeCaregiverIds(PATIENT)).thenReturn(List.of(CAREGIVER));
        when(careTeam.patientName(PATIENT)).thenReturn(Optional.of("Fatma Jlassi"));
    }

    // ---------------------------------------------------------------- helpers

    static RequestPostProcessor user(String sub, String role) {
        return jwt().jwt(j -> j.subject(sub).claim("realm_access", Map.of("roles", List.of(role))))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
    }

    record Device(String id, String key) {
    }

    Device pair(String mac, String zone) throws Exception {
        String body = mvc.perform(post("/api/cameras/pair").with(user(DOCTOR, "DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"%s\",\"macAddress\":\"%s\",\"zone\":\"%s\"}".formatted(PATIENT, mac, zone)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return new Device(node.get("id").asText(), node.get("deviceKey").asText());
    }

    org.springframework.test.web.servlet.ResultActions send(Device d, Instant at, double intensity) throws Exception {
        return mvc.perform(post("/api/events")
                .header("X-Device-Id", d.id()).header("X-Device-Key", d.key())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timestamp\":\"%s\",\"motionIntensity\":%s,\"durationSeconds\":60}".formatted(at, intensity)));
    }

    static Instant tunis(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 11, day, hour, minute).atZone(TUNIS).toInstant();
    }

    // ---------------------------------------------------------------- pairing

    @Test
    void doctorPairsACameraAndGetsTheKeyOnce() throws Exception {
        Device d = pair("aa-bb-cc-dd-ee-01", "HALLWAY");
        var stored = cameras.findById(java.util.UUID.fromString(d.id())).orElseThrow();
        assertThat(stored.getMacAddress()).isEqualTo("AA:BB:CC:DD:EE:01");
        assertThat(stored.getPairedBy()).isEqualTo(DOCTOR);
        assertThat(stored.getDeviceKeyHash()).isNotEqualTo(d.key()).hasSize(64);

        mvc.perform(get("/api/cameras/" + d.id()).with(user(DOCTOR, "DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceKey").doesNotExist())
                .andExpect(jsonPath("$.deviceKeyHash").doesNotExist());
    }

    @Test
    void pairingRules() throws Exception {
        pair("AA:BB:CC:DD:EE:02", "KITCHEN");
        String body = "{\"patientId\":\"%s\",\"macAddress\":\"%s\",\"zone\":\"KITCHEN\"}";
        mvc.perform(post("/api/cameras/pair").with(user(DOCTOR, "DOCTOR")).contentType(MediaType.APPLICATION_JSON)
                .content(body.formatted(PATIENT, "AA:BB:CC:DD:EE:02"))).andExpect(status().isConflict());
        mvc.perform(post("/api/cameras/pair").with(user(DOCTOR, "DOCTOR")).contentType(MediaType.APPLICATION_JSON)
                .content(body.formatted(PATIENT, "not-a-mac"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/cameras/pair").with(user("other-doctor", "DOCTOR")).contentType(MediaType.APPLICATION_JSON)
                .content(body.formatted(PATIENT, "AA:BB:CC:DD:EE:03"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/cameras/pair").with(user(CAREGIVER, "CAREGIVER")).contentType(MediaType.APPLICATION_JSON)
                .content(body.formatted(PATIENT, "AA:BB:CC:DD:EE:04"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/cameras/pair").contentType(MediaType.APPLICATION_JSON)
                .content(body.formatted(PATIENT, "AA:BB:CC:DD:EE:05"))).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanManageAnyPatientsCameras() throws Exception {
        Device d = pair("AA:BB:CC:DD:EE:06", "BEDROOM");
        mvc.perform(get("/api/cameras/patient/" + PATIENT).with(user("admin-1", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(d.id()));
    }

    // ---------------------------------------------------------------- device ingestion

    @Test
    void devicesMustAuthenticateWithTheirKey() throws Exception {
        Device d = pair("AA:BB:CC:DD:EE:10", "HALLWAY");
        String event = "{\"motionIntensity\":0.5,\"durationSeconds\":30}";
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/events").header("X-Device-Id", d.id()).header("X-Device-Key", "wrong")
                .contentType(MediaType.APPLICATION_JSON).content(event)).andExpect(status().isUnauthorized());
        // a signed-in person is not a device: authenticated, but forbidden to post camera events
        mvc.perform(post("/api/events").with(user(DOCTOR, "DOCTOR"))
                .contentType(MediaType.APPLICATION_JSON).content(event)).andExpect(status().isForbidden());
        send(d, NOW.minusSeconds(30), 0.5).andExpect(status().isAccepted());
    }

    @Test
    void firstEventBringsTheCameraOnline() throws Exception {
        Device d = pair("AA:BB:CC:DD:EE:11", "KITCHEN");
        assertThat(cameras.findAll().get(0).getStatus()).isEqualTo(CameraStatus.OFFLINE);
        send(d, NOW.minusSeconds(10), 0.4).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.patternsDetected").isEmpty());
        var camera = cameras.findAll().get(0);
        assertThat(camera.getStatus()).isEqualTo(CameraStatus.ACTIVE);
        assertThat(camera.getLastSeen()).isEqualTo(NOW);
        assertThat(events.findAll()).singleElement().satisfies(e -> {
            assertThat(e.getPatientId()).isEqualTo(PATIENT);     // taken from the pairing, not the device
            assertThat(e.getZone().name()).isEqualTo("KITCHEN");
        });
    }

    @Test
    void eventValidation() throws Exception {
        Device d = pair("AA:BB:CC:DD:EE:12", "HALLWAY");
        send(d, NOW.plusSeconds(3600), 0.5).andExpect(status().isUnprocessableEntity());       // future
        send(d, NOW.minusSeconds(25 * 3600), 0.5).andExpect(status().isUnprocessableEntity()); // > 24 h old
        send(d, NOW, 1.5).andExpect(status().isBadRequest());                                  // intensity > 1
        assertThat(events.count()).isZero();
    }

    @Test
    void pausedCamerasAreNotRecorded() throws Exception {
        Device d = pair("AA:BB:CC:DD:EE:13", "BEDROOM");
        mvc.perform(put("/api/cameras/" + d.id() + "/status").param("status", "PAUSED").with(user(DOCTOR, "DOCTOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAUSED"));
        send(d, NOW, 0.5).andExpect(status().isConflict());
        assertThat(events.count()).isZero();
    }

    // ---------------------------------------------------------------- detection + notification

    @Test
    void wanderingIsDetectedOnceAndTheCareTeamIsNotified() throws Exception {
        Device hall = pair("AA:BB:CC:DD:EE:20", "HALLWAY");
        Device kitchen = pair("AA:BB:CC:DD:EE:21", "KITCHEN");
        Instant start = NOW.minusSeconds(3 * 3600);                    // 10:00 -> 12:16 Tunis
        List<String> detected = new java.util.ArrayList<>();
        for (int i = 0; i < 18; i++) {
            Device d = i % 2 == 0 ? hall : kitchen;
            String body = send(d, start.plusSeconds(8L * 60 * i), 0.6).andExpect(status().isAccepted())
                    .andReturn().getResponse().getContentAsString();
            json.readTree(body).get("patternsDetected").forEach(n -> detected.add(n.asText()));
        }
        assertThat(detected).containsExactly("WANDERING");          // created once, then extended
        var pattern = patterns.findAll().get(0);
        assertThat(pattern.getType()).isEqualTo(PatternType.WANDERING);
        assertThat(pattern.getEventCount()).isEqualTo(18);
        assertThat(pattern.getZones()).isEqualTo("HALLWAY,KITCHEN");

        ArgumentCaptor<NotificationEvent> sent = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(rabbit, timeout(5000).times(2))
                .convertAndSend(eq("alert.notifications"), eq("notification.routing.key"), sent.capture());
        assertThat(sent.getAllValues()).extracting(e -> e.payload().recipientId()).containsExactly(DOCTOR, CAREGIVER);
        NotificationEvent first = sent.getValue();
        assertThat(first.eventType()).isEqualTo("AlertCreated");
        assertThat(first.payload().severity()).isEqualTo("HIGH");
        assertThat(first.payload().ruleCode()).isEqualTo("WANDERING");
        assertThat(first.payload().message()).startsWith("Possible wandering: Fatma Jlassi has been moving between hallway, kitchen since 10:00");
        await(() -> patterns.findAll().get(0).isNotified());
    }

    @Test
    void nightExitAttemptsAreMergedWithinTheSameNight() throws Exception {
        Device door = pair("AA:BB:CC:DD:EE:30", "FRONT_DOOR");
        send(door, tunis(24, 2, 30), 0.8).andExpect(jsonPath("$.patternsDetected[0]").value("NIGHT_EXIT_ATTEMPT"));
        send(door, tunis(24, 3, 5), 0.8).andExpect(jsonPath("$.patternsDetected").isEmpty());
        assertThat(patterns.findAll()).singleElement().satisfies(p -> assertThat(p.getEventCount()).isEqualTo(1));
        verify(rabbit, timeout(5000).times(2)).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));
    }

    @Test
    void failedNotificationsAreRetried() throws Exception {
        Device door = pair("AA:BB:CC:DD:EE:31", "FRONT_DOOR");
        when(careTeam.activeCaregiverIds(PATIENT)).thenThrow(new IllegalStateException("care-team down"));
        send(door, tunis(24, 1, 0), 0.9).andExpect(status().isAccepted());
        Thread.sleep(500);
        assertThat(patterns.findAll().get(0).isNotified()).isFalse();
    }

    @Test
    void doctorReadsAndAcknowledgesPatterns() throws Exception {
        Device door = pair("AA:BB:CC:DD:EE:40", "FRONT_DOOR");
        send(door, tunis(24, 4, 0), 0.7);
        String id = patterns.findAll().get(0).getId().toString();

        mvc.perform(get("/api/patterns/patient/" + PATIENT).with(user(DOCTOR, "DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("NIGHT_EXIT_ATTEMPT"))
                .andExpect(jsonPath("$[0].zones[0]").value("FRONT_DOOR"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
        mvc.perform(get("/api/patterns/patient/" + PATIENT).with(user("other-doctor", "DOCTOR")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/patterns/" + id + "/acknowledge").with(user(DOCTOR, "DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.acknowledgedBy").value(DOCTOR));
        mvc.perform(get("/api/events/patient/" + PATIENT).with(user(DOCTOR, "DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zone").value("FRONT_DOOR"));
    }

    @Test
    void silentCamerasGoOffline() throws Exception {
        Device d = pair("AA:BB:CC:DD:EE:50", "LIVING_ROOM");
        send(d, NOW, 0.3);
        var camera = cameras.findAll().get(0);
        camera.setLastSeen(NOW.minusSeconds(2 * 3600));
        cameras.save(camera);
        assertThat(healthMonitor.markSilentCamerasOffline()).isEqualTo(1);
        assertThat(cameras.findAll().get(0).getStatus()).isEqualTo(CameraStatus.OFFLINE);
    }

    @Test
    void unpairingDeletesTheCameraAndItsEvents() throws Exception {
        Device d = pair("AA:BB:CC:DD:EE:60", "KITCHEN");
        send(d, NOW, 0.3);
        mvc.perform(delete("/api/cameras/" + d.id() + "/unpair").with(user(DOCTOR, "DOCTOR")))
                .andExpect(status().isNoContent());
        assertThat(cameras.count()).isZero();
        assertThat(events.count()).isZero();
        send(d, NOW, 0.3).andExpect(status().isUnauthorized());
    }

    private static void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 50 && !condition.getAsBoolean(); i++) {
            Thread.sleep(100);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }
}
