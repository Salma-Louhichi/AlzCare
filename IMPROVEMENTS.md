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
| SEC-1 | P0 | Backend | Unauthenticated endpoint lets anyone assign a patient to any doctor | ⏳ Planned |
| SEC-2 | P0 | Backend | Services bypass the gateway: identity/notification have no auth and their ports are published | ⏳ Planned |
| SEC-3 | P0 | Repo | Real third-party secrets were in source (Cloudinary secret, TomTom key) | ✅ Removed · ⚠️ rotate keys |
| SEC-4 | P0 | Frontend | Login silently falls back to hard-coded mock users when the backend is unreachable | ⏳ Planned |
| SEC-5 | P0 | Frontend | Route guard checks only "logged in", not the user's role | ⏳ Planned |
| SEC-6 | P1 | Backend | Infrastructure credentials hard-coded in docker-compose | ⏳ Planned |
| SEC-7 | P1 | Backend | Gateway CORS uses an invalid wildcard origin | ⏳ Planned |
| SEC-8 | P1 | Backend | Generated patient passwords are predictable and permanent | ⏳ Planned |
| SEC-9 | P2 | Frontend | OAuth2 password grant + tokens in `localStorage` | ⏳ Planned |
| BLD-1 | P0 | Backend | Services don't compile on current JDKs (Lombok / annotation processing) | ✅ Done |
| BLD-2 | P0 | Backend | Test suites don't compile (stale tests) | ✅ Done |
| BLD-3 | P1 | Repo | No continuous integration | ⏳ Planned |
| BLD-4 | P1 | Backend | Spring Boot 3.2 / Spring Cloud 2023.0.0 are end-of-life | ⏳ Planned |
| BLD-5 | P2 | Backend | No aggregator POM; versions duplicated across 6 POMs | ⏳ Planned |
| FE-1 | P1 | Frontend | Services bypass the auth interceptor and read the token themselves | ⏳ Planned |
| FE-2 | P1 | Frontend | Debug `console.log` statements in production code | ⏳ Planned |
| FE-3 | P1 | Frontend | Zero unit tests | ⏳ Planned |
| FE-4 | P2 | Frontend | Very large components (900+ lines) | ⏳ Planned |
| FE-5 | P2 | Frontend | `any` types and mock `DataService` still used by screens | ⏳ Planned |
| BE-1 | P1 | Backend | `ddl-auto: update/create-drop` instead of migrations outside identity-service | ⏳ Planned |
| BE-2 | P2 | Backend | Notification polling every 30 s instead of push | ⏳ Planned |
| BE-3 | P2 | Backend | Email/SMS/push channels are mocks | ⏳ Planned |
| BE-4 | P2 | Backend | Gateway/compose reference services that are not in the repo | ⏳ Planned |
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

**Fix.**
1. identity-service forwards the doctor's `Authorization` header to care-team.
2. The `permitAll()` rule is removed from both the gateway and care-team, so the path requires a
   JWT with role DOCTOR or ADMIN.
3. care-team checks that a DOCTOR can only create assignments for themselves
   (`jwt.sub == doctorId`); ADMIN may assign for any doctor.

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
- application services and databases are no longer published in docker-compose. Only
  the gateway (8080), Keycloak (8090), and the Eureka/RabbitMQ dashboards are. Debug ports are
  bound to `127.0.0.1` where they are kept.
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

**Fix.** The mock users and the fallback are removed. Network and server errors are shown
to the user as "Authentication service unavailable".

### SEC-5 · Role-less route guard

**Evidence.** `AuthGuard.canActivate` only checks `isAuthenticated()`. A logged-in patient can open
`/admin/users` or `/doctor/...`.

**Fix.** Each area route declares `data: { roles: [...] }`. The guard checks the current
user's role and redirects to `/dashboard` (their own area) when it doesn't match.

---

## P1 — Reliability & maintainability

### SEC-6 · Hard-coded infrastructure credentials
`KEYCLOAK_ADMIN_PASSWORD: admin`, `RABBITMQ_DEFAULT_PASS: alzcare123`,
`CONFIG_SERVER_PASSWORD: config123` and `KC_DB_PASSWORD: keycloak_password` were literal values in
docker-compose. **Fix:** each is now `${VAR:-dev-default}`, documented in `.env.example`,
so a deployment can override all of them without editing tracked files.

### SEC-7 · Invalid CORS wildcard
`configuration.setAllowedOrigins(List.of(..., "http://*:4200"))` doesn't work. Wildcards are only
supported through `setAllowedOriginPatterns`. **Fix:** origins move to configuration
(`alzcare.cors.allowed-origin-patterns`) with sensible local defaults.

### SEC-8 · Weak generated passwords
`DoctorPatientProvisioningService.generatePatientPassword()` returns `"Patient1!" + 8 hex chars`
taken from a UUID, and the password is permanent. **Fix:** the password is now 16
characters from `SecureRandom` and is created as a temporary password, so Keycloak forces a
change at first login.

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

Result: identity 30/30 and notification 11/11 web/unit tests pass. Repository and integration
tests use Testcontainers and run in CI, where Docker is available.

### BLD-3 · No CI
**Fix:** `.github/workflows/ci.yml` runs on every push and pull request. It builds and
tests each backend service (JDK 17, Maven cache) and builds the frontend.

### BLD-4 · End-of-life framework versions
Spring Boot 3.2.x left OSS support in Nov 2024, and Spring Cloud 2023.0.0 is the first, unpatched
release of its train. **Plan:** upgrade to Boot 3.4/3.5 with the matching Spring Cloud 2024.0.x,
moving one service at a time behind CI.

### FE-1 · Token handling spread across services
`patient.service.ts`, `safety-alert.service.ts` and `notification.service.ts` read
`localStorage.getItem('access_token')` and build `Authorization` headers themselves, even though
`authInterceptor` already does this and refreshes tokens. Those requests skip proactive refresh,
so they can fail with an expired token. **Fix:** manual headers are removed and the
interceptor is the single source of truth.

### FE-2 · Debug logging
There are 35 runtime `console.log` calls in `src/`. Several of them print user ids and names
(behavior list and detail modal, camera devices). Eleven admin dashboard and settings buttons
only call a handler that logs "… clicked", so they look interactive but do nothing.
**Fix:** remove the debug logging (`console.error` stays for real errors), and show an explicit
"not available yet" toast from the placeholder buttons until they are implemented.

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
- **OPS-1 · Observability.** The gateway creates `X-Correlation-ID`, and the shared log pattern
  prints `%X{correlationId}`, but no service puts the header into the MDC. Add a small servlet
  filter (or Micrometer Tracing) so one request can be followed across services.
