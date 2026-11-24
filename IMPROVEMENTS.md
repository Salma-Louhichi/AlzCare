# AlzCare — Engineering Review & Improvement Roadmap

_Review date: 2026-11-23 · Scope: `backend/` (6 Spring Boot services + infra) and `frontend/` (Angular 18)_

This document lists what should be improved in AlzCare, ordered by risk. Each item has an
ID, the evidence (file references), why it matters, and the proposed fix. The **Status**
column is updated as items are completed.

Legend — **P0**: security / correctness, fix first · **P1**: reliability & maintainability ·
**P2**: architecture & long-term quality.

---

## Summary

| ID | Priority | Area | Item | Status |
|----|----------|------|------|--------|
| SEC-1 | P0 | Backend | Unauthenticated endpoint lets anyone assign a patient to any doctor | ✅ Done |
| SEC-2 | P0 | Backend | Services bypass the gateway: identity/notification have no auth and their ports are published | ✅ Ports · ⏳ per-service JWT |
| SEC-3 | P0 | Repo | Real third-party secrets were in source (Cloudinary secret, TomTom key) | ✅ Removed · ⚠️ rotate keys |
| SEC-4 | P0 | Frontend | Login silently falls back to hard-coded mock users when the backend is unreachable | ✅ Done |
| SEC-5 | P0 | Frontend | Route guard checks only "logged in", not the user's role | ✅ Done |
| SEC-6 | P1 | Backend | Infrastructure credentials hard-coded in docker-compose | ✅ Done |
| SEC-7 | P1 | Backend | Gateway CORS uses an invalid wildcard origin | ✅ Done |
| SEC-8 | P1 | Backend | Generated patient passwords are predictable and permanent | ✅ Strength · ⏳ temporary (needs SEC-9) |
| SEC-9 | P2 | Frontend | OAuth2 password grant + tokens in `localStorage` | ⏳ Planned |
| BLD-1 | P0 | Backend | Services don't compile on current JDKs (Lombok / annotation processing) | ✅ Done |
| BLD-2 | P0 | Backend | Test suites don't compile (stale tests) | ✅ Done |
| BLD-3 | P1 | Repo | No continuous integration | ✅ Done |
| BLD-4 | P1 | Backend | Spring Boot 3.2 / Spring Cloud 2023.0.0 are end-of-life | ⏳ Planned |
| BLD-5 | P2 | Backend | No aggregator POM; versions duplicated across 6 POMs | ⏳ Planned |
| FE-1 | P1 | Frontend | Services duplicate the auth interceptor and read the token themselves | ✅ Done |
| FE-2 | P1 | Frontend | Debug `console.log` statements in production code | ✅ Done |
| FE-3 | P1 | Frontend | Zero unit tests | ⏳ Planned |
| FE-4 | P2 | Frontend | Very large components (900+ lines) | ⏳ Planned |
| FE-5 | P2 | Frontend | `any` types and mock `DataService` still used by screens | ⏳ Planned |
| BE-1 | P1 | Backend | `ddl-auto: update/create-drop` instead of migrations outside identity-service | ⏳ Planned |
| BE-2 | P2 | Backend | Notification polling every 30 s instead of push | ⏳ Planned |
| BE-3 | P2 | Backend | Email/SMS/push channels are mocks | ⏳ Planned |
| BE-4 | P2 | Backend | Gateway/compose reference services that are not in the repo | ⏳ 2 left (ML, event-ingestion done) |
| IOT-1 | P2 | IoT | Home monitoring hardening: firmware untested, HTTP, no key rotation, caregiver access | ⏳ Planned |
| ML-1 | P2 | ML | Care-risk model trained on synthetic data; needs clinical data + validation | ⏳ Planned |
| OPS-1 | P2 | Backend | Correlation id stops at the gateway | ⏳ Planned |

---

## P0 — Security & correctness

### SEC-1 · Anyone can assign a patient to any doctor (no authentication)

**Evidence**
- `backend/gateway-service/.../config/SecurityConfig.java` —
  `.pathMatchers(HttpMethod.POST, "/api/v1/care-team/doctors/*/patients/create").permitAll()`
- `backend/care-team-service/.../config/SecurityConfig.java` — same path is `permitAll()`.
- `DoctorAssignmentController#createAssignment` takes `doctorId` from the URL and `patientId` from
  the body without checking who is calling.

**Impact.** An unauthenticated attacker can assign any patient to a doctor account they control.
That doctor then gets access to the patient's data through the doctor endpoints. This is a
**broken access control** issue (OWASP A01) on medical data.

**Why it exists.** identity-service calls this endpoint server-to-server during
"doctor creates patient" and did not forward any credentials, so the path was opened up.

**Fix (done).**
1. identity-service forwards the doctor's `Authorization` header to care-team.
2. The `permitAll()` rule is removed from both the gateway and care-team, so the path requires a
   JWT with role DOCTOR or ADMIN.
3. care-team checks that a DOCTOR can only create assignments for themselves
   (`jwt.sub == doctorId`); ADMIN may assign for any doctor.
4. If care-team rejects the assignment or can't be reached, identity-service deletes the Keycloak
   account it just created, so no orphan accounts are left behind.

Side effect: the follow-up `PUT /api/v1/care-team/patients/{id}` (care-profile upsert) was also
sent without a token. It had been failing on every call, logged only as a warning. It now works.
Regression tests: `DoctorAssignmentControllerSecurityTest` (anonymous → 401, other doctor → 403,
patient → 403, self → 200, admin → 200).

### SEC-2 · Services can be called directly, bypassing the gateway

**Evidence**
- identity-service has **no Spring Security dependency**, including `/api/v1/admin/users`
  (create/delete users, reset passwords).
- notification-service `SecurityConfig` is `anyRequest().permitAll()` ("gateway handles auth").
- `docker-compose.yml` publishes every service and database on the host:
  `8001`, `8004`, `8008`, `5432`, `27017`, `3307`, `3308`.

**Impact.** The "gateway does the auth" model only holds if nothing else can reach the services.
Anyone who can reach the host can call `http://host:8001/api/v1/admin/users` with no token.

**Fix.**
- ✅ Done: in docker-compose only the gateway (8080) and Keycloak (8090) listen on all
  interfaces. Every service, database and dashboard port is now bound to `127.0.0.1`, so local
  debugging (Swagger, DB clients) still works but other machines can't reach them. Production
  deployments should not publish them at all.
- ⏳ Next: make every service an OAuth2 resource server (defence in depth). care-team already is.
  identity-service and notification-service should follow, with `@PreAuthorize` on admin
  endpoints.

### SEC-3 · Secrets committed in source

**Evidence (original code, before it was imported here).**
`frontend/src/environments/environment*.ts` contained the Cloudinary `apiKey` **and `apiSecret`**.
`docker-compose.yml` and `care-team-service.yml` used a real TomTom API key as a default value.

**Impact.** A Cloudinary API secret in a browser bundle gives full control of the media account:
delete, overwrite and sign uploads. The key was also visible to anyone who had the original code.

**Fix.**
- ✅ Removed from this repository before the first commit. Unsigned uploads only need
  `cloudName` + `uploadPreset`. The TomTom key now comes only from `TOMTOM_API_KEY`
  (see `backend/.env.example`).
- ⚠️ **Action required:** rotate the Cloudinary API secret and the TomTom key in their
  dashboards. Removing them from code does not invalidate copies that already exist elsewhere.

### SEC-4 · Mock login fallback

**Evidence.** `frontend/src/app/core/services/auth.service.ts`: when Keycloak returns anything
other than 400/401 (network error, 5xx, gateway down), `loginWithKeycloak` falls back to
`loginWithMock`. That method accepts hard-coded users such as `admin@example.com / Password123!`.

**Impact.** In production, an outage lets anyone into the admin UI with a public password. The
backend would reject the fake token, but users would see a "logged-in" UI that behaves
erratically. It also hides real outages.

**Fix (done).** The mock users, `loginWithMock` and `setUseKeycloak` are removed. Network and
server errors now show "Authentication service is unavailable". The login page's
"Demo accounts" shortcuts (real users from the dev realm import) are only shown when
`environment.production` is false.

### SEC-5 · Role-less route guard

**Evidence.** `AuthGuard.canActivate` only checks `isAuthenticated()`. A logged-in patient can open
`/admin/users` or `/doctor/...`.

**Fix (done).** Each area route declares `data: { roles: [...] }`. `AuthGuard` checks the
current user's role and returns a `UrlTree` to `/dashboard` (their own area) when it doesn't match.
Unauthenticated users now go to `/login?returnUrl=…`, so they come back to the page they asked
for. The login page already validates that `returnUrl` is a same-origin path.

---

## P1 — Reliability & maintainability

### SEC-6 · Hard-coded infrastructure credentials
`KEYCLOAK_ADMIN_PASSWORD: admin`, `RABBITMQ_DEFAULT_PASS: alzcare123`,
`CONFIG_SERVER_PASSWORD: config123` and `KC_DB_PASSWORD: keycloak_password` were literal values in
docker-compose. **Fix (done):** each is now `${VAR:-dev-default}`, documented in
`.env.example`. The config repo reads the same variables (Keycloak admin account for
identity-service, RabbitMQ account for notification-service), so one `.env` change applies
everywhere. Before, the docker profiles had their own hard-coded copies that would have silently
overridden the environment.

### SEC-7 · Invalid CORS wildcard
`configuration.setAllowedOrigins(List.of(..., "http://*:4200"))` doesn't work. Wildcards are only
supported through `setAllowedOriginPatterns`. **Fix (done):** the gateway uses
`setAllowedOriginPatterns`, read from `alzcare.cors.allowed-origin-patterns` (env
`ALZCARE_CORS_ALLOWED_ORIGIN_PATTERNS`), defaulting to the local dev origins.
**Follow-up:** CORS is configured twice: in Spring Security and in
`spring.cloud.gateway.globalcors` (config repo). Keep one of them to avoid duplicate
`Access-Control-Allow-*` headers.

### SEC-8 · Weak generated passwords
`DoctorPatientProvisioningService.generatePatientPassword()` returns `"Patient1!" + 8 hex chars`
taken from a UUID, and the password is permanent.
**Fix (done):** `PasswordGenerator` produces 16 characters from `SecureRandom` that always meet
the realm policy (upper, lower, digit, symbol) and avoid look-alike characters. It is covered by
`PasswordGeneratorTest`.
**Deferred:** marking the password *temporary* (forcing a change at first login) would currently
lock patients out. Keycloak rejects the password grant the SPA uses while a required action is
pending, and the app has no change-password screen. This ships together with SEC-9.

### BLD-1 · Build broken on current JDKs
Spring Boot 3.2 pins Lombok 1.18.30, which can't process annotations on JDK 21+. JDK 23 also
disables implicit annotation processing. As a result, `gateway-service` and `care-team-service`
fail with dozens of "cannot find symbol" errors on any developer machine running a recent JDK.
**Fix:** Lombok 1.18.36 everywhere, and explicit `annotationProcessorPaths` in every
module that uses Lombok.

### BLD-2 · Tests don't compile
- identity-service: `PatientProfileControllerTest` calls `setCulturalContext(...)`, but that field
  was removed from the DTOs.
- notification-service: `NotificationScheduleIntegrationTest` imports
  `com.alzcare.notification.service.target.*`, but the package is `...service.targeting`.

The tests can't have run for a while, so regressions went unnoticed. **Fix (done):**
- identity: tests updated to the current model (`assessedBy` is a UUID, `assessedAt` became
  `assessmentDate`, patients are keyed by `userId`, repository finder names). This also uncovered
  a real bug: a malformed UUID in the URL returned **500 "Profile Creation Failed"**. It now
  returns **400 "Invalid Parameter"** through a dedicated `MethodArgumentTypeMismatchException`
  handler.
- notification: fixed the import. `NotificationControllerTest` now loads the service's own
  `SecurityConfig` (it was getting Boot's default security and 401/403 everywhere) and uses an
  argument matcher for a DTO without `equals()`.
- `ScheduledNotificationServiceTest` is `@Disabled` with a reason. It needs a live MongoDB on
  localhost and sleeps for real scheduler ticks. **Follow-up:** rewrite it with Testcontainers
  and a controllable clock.

Result: identity 30/30 and notification 11/11 web/unit tests pass.

**Database-backed tests (run once Docker was available).** Testcontainers 1.19.3 (the Boot 3.2
default) can't talk to Docker Engine 29, so it was pinned to 1.21.4. Running the identity
repository and integration tests against a real PostgreSQL for the first time uncovered two
**production bugs** in `AutonomyAssessment`:
- the `*_level` columns use the PostgreSQL enum type `autonomy_level` but were bound as VARCHAR.
  **No assessment could ever be saved.** Fixed with `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`.
- the score formula produced 11 for an all-INDEPENDENT patient, which violates the
  `CHECK (overall_score BETWEEN 1 AND 10)` constraint. Fixed: `1 + (sum - 4) * 9 / 8`.

It also found two smaller API issues: create responses returned `createdAt`/`updatedAt` as null
(fixed with `saveAndFlush`), and the assessment body required a `patientId` that is ignored in
favour of the URL (now optional). The rest were test-isolation problems (no Keycloak stub,
leftover rows between classes). **Identity: 141/141 tests pass.**

The notification-service Testcontainers suites found two more **production bugs**:
- **RabbitMQ events could never be consumed.** No JSON `MessageConverter` was configured, so
  Spring AMQP used `SimpleMessageConverter`, which only accepts String, byte[] or Serializable.
  `NotificationEvent` is none of these. The asynchronous "alert → notification" path did not
  work. Fixed with a `Jackson2JsonMessageConverter` that infers the target type from the listener.
- **Filtering schedules by role returned only inactive schedules.** The Query-by-Example probe
  had a primitive `boolean active` (default `false`) that `withIgnoreNullValues()` can't skip.
  The `active` path is now ignored unless the caller filters on it.
One more test expected `PENDING` where the API intentionally returns the routed status
(`DELIVERED`). **Notification: 34/34 pass (2 skipped: the live-scheduler test above).**

### BLD-3 · No CI
**Fix (done):** `.github/workflows/ci.yml` runs on every push to `main` and on pull requests:
- one matrix job per backend service: `mvn verify` on Temurin 17 with Maven caching. Surefire
  reports are uploaded when a job fails. Docker on the runner lets the Testcontainers tests run.
- a frontend job: `npm ci` and a production `ng build`.
**Note:** the Testcontainers suites (identity repositories/integration, notification
integration/RabbitMQ) could not be run during this review because there was no local Docker.
The first CI run is their first real run in a long time, so expect fixes there.

### BLD-4 · End-of-life framework versions
Spring Boot 3.2.x left OSS support in Nov 2024, and Spring Cloud 2023.0.0 is the first, unpatched
release of its train. **Plan:** upgrade to Boot 3.4/3.5 with the matching Spring Cloud 2024.0.x,
moving one service at a time behind CI.

### FE-1 · Token handling spread across services
`patient.service.ts`, `safety-alert.service.ts` and `notification.service.ts` each had a private
`getAuthHeaders()` that read `localStorage.getItem('access_token')` and set
`Authorization`/`Content-Type` by hand on 26 requests. `authInterceptor` already attaches (and
overwrites) the bearer token and refreshes it, so this code was redundant. It also sent
`Authorization: Bearer null` when logged out and forced a JSON content type on bodiless requests.
**Fix (done):** the helpers and their headers were removed; the interceptor is the single place
that handles tokens.
Found while doing this: `markAsRead()` called **PATCH** `/notifications/{id}/read`, but the
backend maps that path with `@PutMapping`, so the request could never succeed. It now uses PUT.

### FE-2 · Debug logging
There are 35 runtime `console.log` calls in `src/`. Several of them print user ids and names
(behavior list and detail modal, camera devices). Eleven admin dashboard and settings buttons
only call a handler that logs "… clicked", so they look interactive but do nothing.
**Fix (done):** the 24 debug `console.log` calls were removed (`console.warn`/`console.error` stay
for real problems). Dashboard quick actions now go to the existing page where there is one
(Add patient / Manage users → `/admin/users`, Send broadcast → `/admin/schedules/new`,
Settings → `/admin/settings`). The rest (appointments, reports, settings save, integrations,
backups) show an honest "not available yet" toast instead of failing silently.

### FE-3 · No frontend tests
The project has zero `*.spec.ts` files and no test runner configured, although `npm test`
exists. **Plan:** add Jest (or Karma) and start with `AuthService`, `authInterceptor`,
`AuthGuard` and `NotificationService`: the logic that hurts most when it breaks.

### BE-1 · Schema managed by Hibernate
Only identity-service uses Flyway. care-team, community, event-ingestion and safety use
`ddl-auto: update` or `create-drop`. `create-drop` deletes the data on every restart, and
`update` can't rename or drop columns safely. **Plan:** add Flyway baselines generated from the
current schemas, then switch to `validate`.

---

## P2 — Architecture & long-term quality

- **SEC-9 · Auth flow.** The SPA uses the OAuth2 *Resource Owner Password* grant, which OAuth 2.1
  removes, and stores tokens in `localStorage`, where XSS can read them. Move to *Authorization
  Code + PKCE* (e.g. `angular-auth-oidc-client` or `keycloak-angular`) and keep tokens in memory.
- **BLD-5 · Build structure.** Add a parent `backend/pom.xml` (packaging `pom`) declaring the Boot
  parent, Spring Cloud BOM, Java version and Lombok/MapStruct once. Each service then only
  declares its own dependencies, and `mvn -f backend verify` builds everything.
- **FE-4 · Component size.** `behaviors-page.component.ts` (916 lines and 958 lines of template),
  `admin-care-teams`, `admin-users` and `behavior-detail-modal` mix data loading, filtering and
  presentation. Split each into a container plus presentational components, and move filtering
  into pure functions that can be tested.
- **FE-5 · Typing & mocks.** There are 27 explicit `any` types. `DataService` still serves mock
  patients to some screens. Replace both with typed models and real endpoints, or show clearly
  that the data is a demo.
- **BE-2 · Real-time notifications.** Each logged-in client polls every 30 s. Server-Sent Events
  from notification-service would cut the load and make delivery instant.
- **BE-3 · Delivery channels.** Email/SMS/push only log `[... MOCK]`. Integrate real providers
  (SMTP or SendGrid, Twilio, FCM) behind the existing `NotificationChannelService` interface,
  with feature flags.
- **BE-4 · Missing services.** The gateway rules and compose `full` profile reference
  safety-alert-engine, event-ingestion and community-social-service, but their sources aren't
  in this repo. Add them, or remove their routes so the documentation matches what runs.
  (`ml-prediction` and `event-ingestion` are now implemented: see ML-1 and IOT-1;
  safety-alert-engine and community-social-service remain.)
- **IOT-1 · Home monitoring hardening.** `event-ingestion` (26 tests) receives ESP32 motion
  events authenticated by per-device keys, detects wandering / night motion / night exit
  attempts and alerts the care team through RabbitMQ. The flow was verified end to end with the
  camera simulator. Remaining work:
  - the ESP32 firmware has not been compiled or run on hardware;
  - devices talk HTTP, so production needs TLS with a pinned root CA;
  - there is no device-key rotation endpoint;
  - caregivers can't see their patient's home monitoring yet, because the read endpoints are
    restricted to the doctor and admins;
  - rules are fixed thresholds, not learned per patient.
- **ML-1 · Care-risk model needs real data.** `ml-prediction-service` (Python/FastAPI, 37 tests,
  CI job) serves an explainable logistic-regression model trained on documented **synthetic**
  data (hold-out ROC-AUC 0.787 vs 0.774 for a gradient-boosting baseline). It verifies the JWT
  itself and lets a doctor score only their own patients. Before any real use it needs a
  labelled, consented clinical dataset, external validation, calibration and subgroup checks,
  and drift monitoring (see its MODEL_CARD.md).
- **OPS-1 · Observability.** The gateway creates `X-Correlation-ID`, and the shared log pattern
  prints `%X{correlationId}`, but no service puts the header into the MDC. Add a small servlet
  filter (or Micrometer Tracing) so one request can be followed across services.
