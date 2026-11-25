# AlzCare – Alzheimer's Disease Management Platform

AlzCare is a web platform for Alzheimer's disease management that supports **patients, caregivers, doctors and administrators**.
It combines a Spring Boot microservices backend, an Angular frontend, a Python/FastAPI risk model, and IoT home monitoring with ESP32 motion sensors.

> **Maintainer:** Salma Louhichi — [@Salma-Louhichi](https://github.com/Salma-Louhichi)

---

## Repository layout

```
AlzCare/
├── backend/                   # Spring Boot 3 / Spring Cloud microservices + docker-compose
│   ├── eureka-server/         # Service discovery (8761)
│   ├── config-server/         # Centralised configuration (8888)
│   ├── gateway-service/       # API gateway + JWT validation (8080)
│   ├── identity-service/      # Users & profiles, autonomy assessments, Keycloak admin (8001)
│   ├── event-ingestion/       # IoT: camera events, pattern detection, alerts (8002)
│   ├── notification-service/  # Notifications, schedules, RabbitMQ consumer (8004)
│   ├── care-team-service/     # Care-team assignments, handovers, checklists (8008)
│   ├── ml-prediction-service/ # Python/FastAPI care-risk model (8094)
│   └── keycloak-service/      # Keycloak realm configuration (8090)
├── frontend/                  # Angular 18 standalone app + Tailwind CSS (4200)
├── iot/
│   ├── camera-simulator/      # Python CLI replaying camera scenarios (wandering, night exit…)
│   └── esp32-firmware/        # Reference ESP32 + PIR sketch (not hardware-tested)
├── docs/                      # Architecture documentation
└── IMPROVEMENTS.md            # Engineering review & improvement roadmap
```

## Features

Features marked ⚠️ have a frontend only: their backend service is **not in this repository**
(see BE-4 in [IMPROVEMENTS.md](IMPROVEMENTS.md)). Those screens use sample data or show an error.

### Doctor
- **My patients** – Patients assigned to the doctor through care-team-service
- **Autonomy assessments** – Record hygiene / medication / mobility / feeding autonomy and follow the 1–10 score over time
- **Home monitoring (IoT)** – ESP32 sensors report motion only, never images. The platform detects
  wandering, night activity and night exit attempts and alerts the doctor and the caregivers.
  The doctor can acknowledge an alert, pause or resume a room's camera, and see activity per room
  over the last 24 h
- **Care-risk estimate (ML)** – Probability that the next assessment shows a decline, with the
  three main factors explained ([model card](backend/ml-prediction-service/MODEL_CARD.md):
  demonstration model trained on synthetic data, not clinically validated)
- **Checklists** – Care checklists per patient

### Caregiver
- **Assigned patients and handovers** – Care-team assignments and shift handovers between caregivers
- **Tasks** – Daily task list
- ⚠️ **Behaviour tracking** – Log behaviours with photos (Cloudinary upload); needs safety-alert-engine

### Patient
- **Dashboard, medications, activities, profile** – Screens built with sample data (no backend yet)
- **Brain games** – Cognitive exercises in the browser
- ⚠️ **Community forum** – Posts, comments, likes and categories; needs community-social-service

### Admin
- **User management** – Create, edit and delete patients, caregivers, doctors and admins.
  Accounts are created in Keycloak at the same time, with a generated password
- **Care teams** – Assign doctors and caregivers to patients
- **Camera pairing** – Pair an ESP32 sensor to a patient's room; the device key is shown once
- **Notification schedules** – Create, toggle and trigger scheduled notification campaigns
- ⚠️ **Community moderation**, analytics

### Shared
- **Notifications** – Bell with polling, toasts and a notification page (notification-service, MongoDB)
- **Role-based routing** – `/dashboard` redirects to the right space; each space is guarded by role
- **Token refresh** – Keycloak OAuth2 / OpenID Connect

---

## Tech stack

| Area | Technologies |
|---|---|
| Frontend | Angular 18 (standalone components), TypeScript, Tailwind CSS, RxJS |
| Backend | Java 17, Spring Boot 3.2, Spring Cloud 2023 (Gateway, Eureka, Config), Spring Security OAuth2, OpenFeign |
| ML | Python 3.12, FastAPI, scikit-learn, PyJWT |
| IoT | ESP32 (Arduino) + PIR sensor, Python camera simulator |
| Auth | Keycloak 22 (realm `alzcare`, roles PATIENT / CAREGIVER / DOCTOR / ADMIN) |
| Messaging | RabbitMQ 3.12 (exchange `alert.notifications`, dead-letter queue) |
| Databases | PostgreSQL 15 (identity, event-ingestion, Keycloak; Flyway), MySQL 8 (care-team), MongoDB 6 (notifications) |
| Testing | JUnit 5, Mockito, Testcontainers, pytest |
| DevOps | Docker, Docker Compose, GitHub Actions |

---

## Architecture

Microservices behind a single gateway, with service discovery and centralised configuration.

```
                Angular app (4200)              ESP32 sensors / camera simulator
                      │  JWT (Keycloak)                │  X-Device-Id + X-Device-Key
                      ▼                                ▼
               ┌──────────────────── Gateway (8080) ────────────────────┐
               │      JWT validation (JWKS), role rules, routing        │
               └──┬──────────┬──────────────┬─────────────┬──────────┬──┘
                  ▼          ▼              ▼             ▼          ▼
             identity   care-team    event-ingestion  notification  ml-prediction
              (8001)      (8008)         (8002)          (8004)       (8094)
             PostgreSQL   MySQL        PostgreSQL       MongoDB     scikit-learn
                                            │               ▲
                                            └── RabbitMQ ───┘
                                           alert.notifications

  Infrastructure: Eureka (8761) · Config Server (8888) · Keycloak (8090) · RabbitMQ (5672)
```

### Communication
- **Client → services**: REST through the gateway, which checks the Keycloak JWT and the role.
- **Service → service**: REST with the caller's token forwarded (ml-prediction → identity and
  care-team), OpenFeign (notification → identity), and a client-credentials token for
  event-ingestion's own background calls (Keycloak client `alzcare-event-ingestion`).
- **Asynchronous**: event-ingestion publishes `AlertCreated` messages to RabbitMQ
  `alert.notifications`; notification-service consumes them.
- **Devices**: sensors authenticate with a per-device key (256-bit; only its SHA-256 hash is stored).

The gateway also defines routes for services that are not in this repository
(safety-alert-engine, cognitive-memory, daily-care, medical-management, community-social-service).
See [docs/architecture](docs/architecture) and BE-4 in [IMPROVEMENTS.md](IMPROVEMENTS.md).

---

## Quality

- **Tests**: about 250 automated tests. Java: identity 141, notification 34, event-ingestion 26,
  care-team 5 (integration tests on real databases with Testcontainers). Python: ml-prediction 37,
  camera simulator 7.
- **CI**: GitHub Actions runs 10 jobs on every push: build and test of 7 backend services, ML
  training and tests with a Docker build, simulator tests, and a production frontend build.
- The Angular frontend has no unit tests yet (FE-3).

---

## Quick start

```bash
git clone https://github.com/Salma-Louhichi/AlzCare.git
cd AlzCare

# 1. Backend
cd backend
cp .env.example .env          # then fill in your own values
docker compose up -d          # core stack (+ `--profile full` for optional services)

# 2. Frontend
cd ../frontend
npm install
npm start                     # http://localhost:4200 (proxies /api to the gateway)
```

Open http://localhost:4200. In local development the login page offers demo accounts
(patient, caregiver, doctor and admin) from the imported Keycloak realm.

**Ports already in use?** If something else on your machine uses 8080 or 27017, set
`GATEWAY_HOST_PORT` / `MONGO_HOST_PORT` in `backend/.env` (e.g. 8081 / 27018) and start the
frontend with `GATEWAY_URL=http://localhost:8081 npm start`.

Stop everything with `docker compose down` in `backend/` (add `-v` to also delete the data).

**Try the home monitoring without hardware** (after creating a patient as the demo doctor):

```bash
cd iot/camera-simulator && pip install -r requirements.txt
python simulate.py setup --patient <patient-user-id>   # pairs one simulated camera per room
python simulate.py run wandering                       # also: night-motion, night-exit, quiet-day
python simulate.py patterns                            # alerts detected for the patient
```

Add `--gateway http://localhost:8081` if you changed the gateway port. The alerts appear on the
patient's page (doctor › My patients) under **Home monitoring**.

### Known limitations of home monitoring
- The ESP32 firmware has **not been compiled or run on real hardware**. The platform side is
  tested end to end with the simulator.
- Devices talk HTTP; production needs TLS. Device keys cannot be rotated yet (re-pair instead).
- Caregivers receive the alerts as notifications, but only doctors and admins can open the
  monitoring panel.

See [backend/README.md](backend/README.md), [frontend/README.md](frontend/README.md),
[event-ingestion](backend/event-ingestion/README.md) and
[ml-prediction-service](backend/ml-prediction-service/README.md) for details.

---

## Author

**Salma Louhichi** — [@Salma-Louhichi](https://github.com/Salma-Louhichi)

AlzCare originated as a 4th-year engineering project (PIDEV 4SAE, 2025–2026) at Esprit School of Engineering, Tunisia.
