# event-ingestion

Spring Boot service (port **8002**, PostgreSQL `postgres-event`) that receives motion events from
ESP32 home cameras, detects risky behaviour and alerts the patient's care team.

```
ESP32 camera ──POST /api/events (X-Device-Id + X-Device-Key)──▶ gateway ──▶ event-ingestion
                                                                             │ store event (PostgreSQL)
                                                                             │ detect patterns
                                                                             ▼ after commit
                       care-team-service ◀── who cares for the patient? ── PatternNotifier
                                                                             │ one message per recipient
                                                                             ▼
                              RabbitMQ alert.notifications ──▶ notification-service ──▶ bell / email…
```

## Privacy
Only motion metadata is stored (time, zone, intensity, duration) — never images or video.
A camera set to **PAUSED** has its events rejected, not stored.

## Device security
- Pairing (`POST /api/cameras/pair`) returns a 256-bit **device key once**; only its SHA-256
  hash is stored. It is compared in constant time.
- Cameras authenticate with `X-Device-Id` + `X-Device-Key`. The patient and zone come from the
  pairing record, so a device cannot report for another patient or room.
- Events more than 5 min in the future or older than 24 h are rejected.

## Detection rules
| Pattern | Rule | Severity |
|---|---|---|
| `WANDERING` | Continuous motion (gaps ≤ 10 min) for ≥ 2 h across ≥ 2 zones, ≥ 6 events | HIGH |
| `NIGHT_MOTION` | 22:00–06:00, ≥ 3 events outside the bedroom within 30 min | MEDIUM |
| `NIGHT_EXIT_ATTEMPT` | Any front-door motion 22:00–06:00 | HIGH |

Thresholds and the time zone (default `Africa/Tunis`) are configurable (`alzcare.monitoring.*`).
A wandering episode that continues, or a second night event in the same night, **extends** the
existing pattern instead of creating a new alert. Events may arrive out of order (a camera that
was offline sends its buffer), so a run is grown in both directions from each new event.

## Notifications
After the database commit, asynchronously, the service looks up the patient's active doctor and
active caregivers in care-team-service, using its own client-credentials token (Keycloak client
`alzcare-event-ingestion`). It then publishes one `AlertCreated` message per recipient to
RabbitMQ. If that fails, a job retries every minute for 24 h.

## API
| Method | Path | Who |
|---|---|---|
| POST | `/api/events` | camera (device key) |
| POST | `/api/cameras/pair` | DOCTOR (own patients), ADMIN |
| GET | `/api/cameras/patient/{patientId}`, `/api/cameras/{id}` | DOCTOR (own), ADMIN |
| PUT | `/api/cameras/{id}/status?status=ACTIVE\|PAUSED` | DOCTOR (own), ADMIN |
| DELETE | `/api/cameras/{id}/unpair` | DOCTOR (own), ADMIN |
| GET | `/api/events/patient/{patientId}?hours=24` | DOCTOR (own), ADMIN |
| GET | `/api/patterns/patient/{patientId}?days=30` | DOCTOR (own), ADMIN |
| PUT | `/api/patterns/{id}/acknowledge` | DOCTOR (own), ADMIN |

Cameras silent for more than 60 min are marked `OFFLINE` (checked every 5 min).

## Tests
`mvn verify` — 26 tests: detector unit tests (wandering, gaps, out-of-order events, night
window across midnight…), device-key tests, and integration tests through HTTP and both security
chains against PostgreSQL (Testcontainers) with a fixed clock.
