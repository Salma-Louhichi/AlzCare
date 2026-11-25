# AlzCare – Backend

Spring Boot 3.2 / Spring Cloud microservices for the AlzCare platform. Part of the [AlzCare monorepo](../README.md).

## Services in this repository

| Service | Port | Stack | Database | Role |
|---|---|---|---|---|
| eureka-server | 8761 | Spring Cloud Netflix | – | Service discovery |
| config-server | 8888 | Spring Cloud Config (native) | – | Central configuration (`config-repo/`) |
| gateway-service | 8080 | Spring Cloud Gateway (WebFlux) | – | Routing, JWT validation, role rules, CORS |
| identity-service | 8001 | Spring Boot, Flyway | PostgreSQL | Users, profiles, autonomy assessments, Keycloak admin |
| event-ingestion | 8002 | Spring Boot, Flyway | PostgreSQL | Camera pairing, motion events, pattern detection ([README](event-ingestion/README.md)) |
| notification-service | 8004 | Spring Boot, OpenFeign | MongoDB | Notifications, schedules, RabbitMQ consumer |
| care-team-service | 8008 | Spring Boot | MySQL | Doctor/caregiver assignments, handovers, checklists |
| ml-prediction-service | 8094 | Python, FastAPI, scikit-learn | – | Care-risk model ([README](ml-prediction-service/README.md)) |
| keycloak | 8090 | Keycloak 22 | PostgreSQL | Realm `alzcare`, roles PATIENT / CAREGIVER / DOCTOR / ADMIN |
| rabbitmq | 5672 / 15672 | RabbitMQ 3.12 | – | Exchange `alert.notifications`, dead-letter queue |

The gateway also has routes for services whose code is **not** in this repository
(safety-alert-engine 8003, cognitive-memory 8005, daily-care 8006, medical-management 8007,
community-social-service 8009). Requests to them fail until they exist.

### Communication
- **Client → service**: REST through the gateway, which validates the Keycloak JWT (JWKS) and the role.
- **Service → service**: REST with the user's token forwarded (ml-prediction → identity and
  care-team), OpenFeign (notification → identity), and a client-credentials token for
  event-ingestion's background calls to care-team (Keycloak client `alzcare-event-ingestion`).
- **Asynchronous**: event-ingestion publishes `AlertCreated` messages to RabbitMQ
  `alert.notifications`; notification-service consumes them.
- **Devices**: ESP32 sensors call `POST /api/events` with `X-Device-Id` / `X-Device-Key`.

---


## Running locally

```bash
cp .env.example .env
# Start infrastructure first
docker compose up -d eureka-server config-server rabbitmq postgres-keycloak keycloak
# Then the application services
docker compose up -d
```

`event-ingestion` (with its `postgres-event` database) and `ml-prediction` are part of the
default stack. Services that are referenced by the gateway but not part of this repository
(`safety-alert-engine`, `community-social-service`) are behind the `full` compose profile:
`docker compose --profile full up -d`.

### Access Points
| Service | URL | Credentials |
|---------|-----|-------------|
| Web Application | http://localhost:4200 | - |
| API Gateway | http://localhost:8080 | JWT Token |
| Eureka Dashboard | http://localhost:8761 | - |
| RabbitMQ Management | http://localhost:15672 | see docker-compose.yml |
| Keycloak Admin | http://localhost:8090 | see docker-compose.yml (dev only) |

---

