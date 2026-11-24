"""Motion timelines that reproduce what home cameras would report.

Every scenario is a list of MotionEvent in time order. Timestamps are in the past (the
service accepts events up to 24 h old), so hours of activity can be replayed in seconds.
"""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, time, timedelta
from zoneinfo import ZoneInfo

TUNIS = ZoneInfo("Africa/Tunis")


@dataclass(frozen=True)
class MotionEvent:
    zone: str
    at: datetime
    intensity: float
    duration_seconds: int


def _last_night(now: datetime, hour: int, minute: int, tz: ZoneInfo) -> datetime:
    """Most recent occurrence of hour:minute (local time) that is in the past."""
    local_now = now.astimezone(tz)
    candidate = datetime.combine(local_now.date(), time(hour, minute), tz)
    if candidate >= local_now:
        candidate -= timedelta(days=1)
    return candidate


def quiet_day(now: datetime, tz: ZoneInfo = TUNIS) -> list[MotionEvent]:
    """Normal daytime life: a few short movements far apart. Expected: no pattern."""
    start = now - timedelta(hours=5)
    plan = [("KITCHEN", 0), ("LIVING_ROOM", 50), ("KITCHEN", 110), ("BATHROOM", 170), ("LIVING_ROOM", 240)]
    return [MotionEvent(zone, start + timedelta(minutes=m), 0.3, 90) for zone, m in plan]


def wandering(now: datetime, tz: ZoneInfo = TUNIS) -> list[MotionEvent]:
    """Restless walking between rooms every 7 minutes for 2 h 20. Expected: WANDERING (HIGH)."""
    start = now - timedelta(hours=2, minutes=30)
    zones = ["HALLWAY", "KITCHEN", "LIVING_ROOM", "HALLWAY"]
    return [MotionEvent(zones[i % len(zones)], start + timedelta(minutes=7 * i), 0.6, 120) for i in range(21)]


def night_motion(now: datetime, tz: ZoneInfo = TUNIS) -> list[MotionEvent]:
    """Up at 02:30 last night, moving between hallway, kitchen and living room. Expected: NIGHT_MOTION."""
    start = _last_night(now, 2, 30, tz)
    plan = [("BEDROOM", 0), ("HALLWAY", 3), ("KITCHEN", 9), ("LIVING_ROOM", 16), ("HALLWAY", 22)]
    return [MotionEvent(zone, start + timedelta(minutes=m), 0.5, 60) for zone, m in plan]


def night_exit(now: datetime, tz: ZoneInfo = TUNIS) -> list[MotionEvent]:
    """Hallway then front door at 03:10 last night. Expected: NIGHT_EXIT_ATTEMPT (HIGH)."""
    start = _last_night(now, 3, 8, tz)
    return [MotionEvent("HALLWAY", start, 0.6, 40),
            MotionEvent("FRONT_DOOR", start + timedelta(minutes=2), 0.8, 30)]


SCENARIOS = {
    "quiet-day": quiet_day,
    "wandering": wandering,
    "night-motion": night_motion,
    "night-exit": night_exit,
}
