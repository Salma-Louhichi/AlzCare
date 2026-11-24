package com.alzcare.eventingestion.detection;

import com.alzcare.eventingestion.config.MonitoringProperties;
import com.alzcare.eventingestion.domain.CameraZone;
import com.alzcare.eventingestion.domain.PatternType;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Rule-based detection of risky behaviour from motion events (pure logic, no I/O).
 *
 * <ul>
 *   <li><b>WANDERING</b> - a continuous run of motion (gaps of at most {@code wanderingMaxGap})
 *       lasting at least {@code wanderingMinDuration}, across at least {@code wanderingMinZones}
 *       zones, with at least {@code wanderingMinEvents} events.</li>
 *   <li><b>NIGHT_MOTION</b> - at night, at least {@code nightMotionMinEvents} events outside the
 *       bedroom within {@code nightMotionWindow}.</li>
 *   <li><b>NIGHT_EXIT_ATTEMPT</b> - any motion at the front door at night.</li>
 * </ul>
 *
 * Each call looks at the pattern the new event belongs to. Events may arrive out of order
 * (devices buffer while offline), so the run is grown in both directions from the new event.
 */
public class PatternDetector {

    private final MonitoringProperties props;

    public PatternDetector(MonitoringProperties props) {
        this.props = props;
    }

    /**
     * @param recent  the patient's events around {@code current}, including {@code current}
     * @param current the event that was just received
     */
    public List<PatternCandidate> detect(List<MotionSample> recent, MotionSample current) {
        List<MotionSample> samples = new ArrayList<>(recent);
        samples.sort(Comparator.comparing(MotionSample::at));
        List<PatternCandidate> found = new ArrayList<>();
        detectWandering(samples, current, found);
        if (isNight(current.at())) {
            detectNightExit(current, found);
            detectNightMotion(samples, current, found);
        }
        return found;
    }

    public boolean isNight(Instant instant) {
        LocalTime t = instant.atZone(props.timezone()).toLocalTime();
        LocalTime start = props.nightStart();
        LocalTime end = props.nightEnd();
        return start.isAfter(end)
                ? !t.isBefore(start) || t.isBefore(end)    // window crosses midnight (22:00-06:00)
                : !t.isBefore(start) && t.isBefore(end);
    }

    private void detectWandering(List<MotionSample> samples, MotionSample current, List<PatternCandidate> found) {
        int index = samples.indexOf(current);
        if (index < 0) {
            return;
        }
        Duration maxGap = props.wanderingMaxGap();
        int first = index;
        while (first > 0 && gap(samples.get(first - 1), samples.get(first)).compareTo(maxGap) <= 0) {
            first--;
        }
        int last = index;
        while (last < samples.size() - 1 && gap(samples.get(last), samples.get(last + 1)).compareTo(maxGap) <= 0) {
            last++;
        }
        List<MotionSample> run = samples.subList(first, last + 1);
        Instant start = run.get(0).at();
        Instant end = run.stream().map(MotionSample::end).max(Comparator.naturalOrder()).orElseThrow();
        LinkedHashSet<CameraZone> zones = zonesOf(run);

        if (Duration.between(start, end).compareTo(props.wanderingMinDuration()) >= 0
                && zones.size() >= props.wanderingMinZones()
                && run.size() >= props.wanderingMinEvents()) {
            found.add(new PatternCandidate(PatternType.WANDERING, start, end, run.size(), zones));
        }
    }

    private void detectNightExit(MotionSample current, List<PatternCandidate> found) {
        if (current.zone() == CameraZone.FRONT_DOOR) {
            LinkedHashSet<CameraZone> zones = new LinkedHashSet<>(List.of(CameraZone.FRONT_DOOR));
            found.add(new PatternCandidate(PatternType.NIGHT_EXIT_ATTEMPT, current.at(), current.end(), 1, zones));
        }
    }

    private void detectNightMotion(List<MotionSample> samples, MotionSample current, List<PatternCandidate> found) {
        if (current.zone() == CameraZone.BEDROOM) {
            return;
        }
        Instant windowStart = current.at().minus(props.nightMotionWindow());
        List<MotionSample> window = samples.stream()
                .filter(s -> !s.at().isBefore(windowStart) && !s.at().isAfter(current.at()))
                .filter(s -> s.zone() != CameraZone.BEDROOM && isNight(s.at()))
                .toList();
        if (window.size() >= props.nightMotionMinEvents()) {
            found.add(new PatternCandidate(PatternType.NIGHT_MOTION, window.get(0).at(), current.end(),
                    window.size(), zonesOf(window)));
        }
    }

    private static Duration gap(MotionSample earlier, MotionSample later) {
        Duration d = Duration.between(earlier.end(), later.at());
        return d.isNegative() ? Duration.ZERO : d;
    }

    private static LinkedHashSet<CameraZone> zonesOf(List<MotionSample> samples) {
        LinkedHashSet<CameraZone> zones = new LinkedHashSet<>();
        samples.forEach(s -> zones.add(s.zone()));
        return zones;
    }
}
