"""The scenarios must match the service's detection rules (see event-ingestion README)."""
from datetime import datetime, timedelta, timezone

import pytest

from scenarios import SCENARIOS, TUNIS, night_exit, night_motion, quiet_day, wandering

NOW = datetime(2026, 11, 24, 12, 0, tzinfo=timezone.utc)   # 13:00 in Tunis


def is_night(dt):
    t = dt.astimezone(TUNIS).time()
    return t.hour >= 22 or t.hour < 6


@pytest.mark.parametrize("name", sorted(SCENARIOS))
def test_events_are_ordered_in_the_past_and_within_24h(name):
    events = SCENARIOS[name](NOW)
    assert events == sorted(events, key=lambda e: e.at)
    assert all(NOW - timedelta(hours=24) < e.at < NOW for e in events)
    assert all(0 <= e.intensity <= 1 and 1 <= e.duration_seconds <= 3600 for e in events)


def test_wandering_satisfies_the_wandering_rule():
    events = wandering(NOW)
    end = events[-1].at + timedelta(seconds=events[-1].duration_seconds)
    gaps = [(b.at - (a.at + timedelta(seconds=a.duration_seconds))) for a, b in zip(events, events[1:])]
    assert end - events[0].at >= timedelta(hours=2)
    assert max(gaps) <= timedelta(minutes=10)
    assert len({e.zone for e in events}) >= 2 and len(events) >= 6
    assert not any(is_night(e.at) for e in events)


def test_night_scenarios_happen_at_night():
    assert all(is_night(e.at) for e in night_motion(NOW) + night_exit(NOW))
    outside_bedroom = [e for e in night_motion(NOW) if e.zone != "BEDROOM"]
    assert len(outside_bedroom) >= 3
    assert outside_bedroom[-1].at - outside_bedroom[0].at <= timedelta(minutes=30)
    assert night_exit(NOW)[-1].zone == "FRONT_DOOR"


def test_quiet_day_stays_below_every_threshold():
    events = quiet_day(NOW)
    gaps = [b.at - a.at for a, b in zip(events, events[1:])]
    assert min(gaps) > timedelta(minutes=10)
    assert not any(is_night(e.at) for e in events) and "FRONT_DOOR" not in {e.zone for e in events}
