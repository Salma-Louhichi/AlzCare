package com.alzcare.eventingestion.detection;

import com.alzcare.eventingestion.config.MonitoringProperties;
import com.alzcare.eventingestion.domain.CameraZone;
import com.alzcare.eventingestion.domain.PatternType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static com.alzcare.eventingestion.domain.CameraZone.*;
import static org.assertj.core.api.Assertions.assertThat;

class PatternDetectorTest {

    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");
    private final PatternDetector detector = new PatternDetector(MonitoringProperties.defaults());

    private static Instant at(int hour, int minute) {
        return LocalDateTime.of(2026, 11, 24, hour, minute).atZone(TUNIS).toInstant();
    }

    /** One event every {@code everyMinutes}, cycling through the zones, from start for {@code count} events. */
    private static List<MotionSample> walk(Instant start, int count, int everyMinutes, CameraZone... zones) {
        List<MotionSample> samples = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            samples.add(new MotionSample(start.plusSeconds(60L * everyMinutes * i), zones[i % zones.length], 60));
        }
        return samples;
    }

    private List<PatternType> types(List<MotionSample> samples, MotionSample current) {
        return detector.detect(samples, current).stream().map(PatternCandidate::type).toList();
    }

    @Test
    void continuousMovementAcrossRoomsForTwoHoursIsWandering() {
        List<MotionSample> samples = walk(at(14, 0), 17, 8, HALLWAY, KITCHEN, LIVING_ROOM);   // 14:00 -> 16:08
        PatternCandidate c = detector.detect(samples, samples.get(samples.size() - 1)).get(0);
        assertThat(c.type()).isEqualTo(PatternType.WANDERING);
        assertThat(c.eventCount()).isEqualTo(17);
        assertThat(c.zones()).containsExactly(HALLWAY, KITCHEN, LIVING_ROOM);
    }

    @Test
    void shortActivityIsNotWandering() {
        List<MotionSample> samples = walk(at(14, 0), 12, 8, HALLWAY, KITCHEN);                // 1 h 29
        assertThat(types(samples, samples.get(samples.size() - 1))).isEmpty();
    }

    @Test
    void longActivityInOneRoomIsNotWandering() {
        List<MotionSample> samples = walk(at(14, 0), 20, 8, KITCHEN);
        assertThat(types(samples, samples.get(samples.size() - 1))).isEmpty();
    }

    @Test
    void aBreakLongerThanTheMaxGapEndsTheRun() {
        List<MotionSample> morning = walk(at(9, 0), 9, 8, HALLWAY, KITCHEN);                 // 09:00-10:05
        List<MotionSample> later = walk(at(10, 30), 9, 8, HALLWAY, KITCHEN);                 // 25 min break
        List<MotionSample> all = new ArrayList<>(morning);
        all.addAll(later);
        assertThat(types(all, all.get(all.size() - 1))).isEmpty();
    }

    @Test
    void outOfOrderEventStillCompletesTheRun() {
        List<MotionSample> samples = walk(at(14, 0), 17, 8, HALLWAY, KITCHEN);
        MotionSample lateArrival = samples.get(5);                                             // buffered by the device
        assertThat(types(samples, lateArrival)).containsExactly(PatternType.WANDERING);
    }

    @Test
    void frontDoorAtNightIsAnExitAttempt() {
        MotionSample door = new MotionSample(at(2, 30), FRONT_DOOR, 20);
        assertThat(types(List.of(door), door)).containsExactly(PatternType.NIGHT_EXIT_ATTEMPT);
    }

    @Test
    void frontDoorDuringTheDayIsNormal() {
        MotionSample door = new MotionSample(at(11, 0), FRONT_DOOR, 20);
        assertThat(types(List.of(door), door)).isEmpty();
    }

    @Test
    void repeatedNightMovementOutsideTheBedroomIsNightMotion() {
        List<MotionSample> samples = List.of(
                new MotionSample(at(3, 0), HALLWAY, 30),
                new MotionSample(at(3, 10), KITCHEN, 30),
                new MotionSample(at(3, 20), HALLWAY, 30));
        assertThat(types(samples, samples.get(2))).containsExactly(PatternType.NIGHT_MOTION);
    }

    @Test
    void bedroomMovementAtNightIsIgnored() {
        List<MotionSample> samples = walk(at(1, 0), 5, 5, BEDROOM);
        assertThat(types(samples, samples.get(4))).isEmpty();
    }

    @Test
    void aSingleBathroomTripAtNightIsNormal() {
        List<MotionSample> samples = List.of(
                new MotionSample(at(3, 0), HALLWAY, 20),
                new MotionSample(at(3, 2), BATHROOM, 120));
        assertThat(types(samples, samples.get(1))).isEmpty();
    }

    @Test
    void nightWindowCrossesMidnight() {
        assertThat(detector.isNight(at(22, 0))).isTrue();
        assertThat(detector.isNight(at(23, 59))).isTrue();
        assertThat(detector.isNight(at(0, 30))).isTrue();
        assertThat(detector.isNight(at(5, 59))).isTrue();
        assertThat(detector.isNight(at(6, 0))).isFalse();
        assertThat(detector.isNight(at(21, 59))).isFalse();
    }
}
