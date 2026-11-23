# ALZCARE PLATFORM - LOGICAL ARCHITECTURE

> High-level system design describing components, layers, and their relationships | v1.0 | Feb 2026

## 1. Architectural Overview

AlzCare follows a **Layered Microservices Architecture** with:
- **API Gateway** for unified entry point
- **Service Discovery** for dynamic routing
- **Event-Driven Communication** via message broker
- **Database-per-Service** pattern
- **CQRS** where applicable (event ingestion)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT LAYER                                    │
│         Angular Web App (All Roles)          │  ESP32 Cameras (IoT)                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           GATEWAY LAYER                                      │
│              Spring Cloud Gateway (Routing, Auth, Rate Limiting)             │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                       ┌───────────────┴───────────────┐
                       ▼                               ▼
┌─────────────────────────────┐            ┌─────────────────────────────┐
│   SERVICE DISCOVERY         │            │   CONFIGURATION             │
│   (Eureka Server)           │            │   (Config Server)           │
│   ──► all microservices     │            │   ···► all microservices    │
└─────────────────────────────┘            └─────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SECURITY LAYER                                       │
│                        KEYCLOAK (OAuth2/OIDC)                                │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      MICROSERVICES LAYER (10 Services)                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │ Identity │ │  Event   │ │  Safety  │ │ Notification│ │ Cognitive│          │
│  │ Service  │ │ Ingestion│ │  Alert   │ │  Service    │ │ & Memory │          │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘          │
│       │            │            │            │            │                 │
│       └────────────┴────────────┼────────────┴────────────┘                 │
│                                 ▼                                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │  Daily   │ │ Medical  │ │ Care     │ │ Community│ │    ML    │          │
│  │  Care    │ │Management│ │  Team    │ │  Social  │ │ Service  │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────┬─────┘          │
│                                                           │                 │
└───────────────────────────────────────────────────────────┼─────────────────┘
                                                            │
                              ┌─────────────────────────────┼─────────────┐
                              │                             │             │
                              ▼                             ▼             ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         MESSAGE BUS (RabbitMQ)                               │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─   │
│     Event Ingestion ──► RabbitMQ ──► Safety Alert Engine                    │
│                                     RabbitMQ ──► Notification Service       │
└─────────────────────────────────────────────────────────────────────────────┘
                                                            │
                                                            ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DATA LAYER                                         │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌─────────────────────────┐   │
│  │ PostgreSQL │ │   MySQL    │ │  MongoDB   │ │      External APIs      │   │
│  │ (Primary)  │ │ (Secondary)│ │ (Documents)│ │  (Cloudinary, Twilio...)│   │
│  └────────────┘ └────────────┘ └────────────┘ └─────────────────────────┘   │
│                                                                             │
│  ════════════════════════════════════════════════════════════════════════   │
│                              JDBC / SQL Connections                         │
└─────────────────────────────────────────────────────────────────────────────┘

LEGEND:
  ─── Solid   │ = HTTP/REST
  ··· Dotted  │ = Config/Discovery
  ─ ─ Dashed  │ = Async (RabbitMQ)
  ═══         │ = JDBC/DB Connection
```

---

## 2. Layer Descriptions

### 2.1 Client Layer

**Components:**
- **Angular Web App** - Unified interface for all roles (Patient/Caregiver/Doctor/Admin)
- **ESP32 IoT Cameras** - Behavior monitoring devices (HTTP POST to event-ingestion)

**Responsibilities:**
- User interaction and presentation
- JWT token storage (HttpOnly cookies)
- Real-time updates via WebSocket

### 2.2 Gateway Layer

**Component:** Spring Cloud Gateway

**Responsibilities:**
| Function | Description |
|----------|-------------|
| Dynamic Routing | Route `/api/v1/patients/**` → identity-service |
| JWT Validation | Verify tokens with Keycloak public key |
| Rate Limiting | Prevent abuse (100 req/min per user) |
| SSL Termination | HTTPS at edge |
| Path Rewriting | Strip `/api/v1` before forwarding |
| CORS Handling | Allow cross-origin from web apps |

**Routing Table:**
```
/identity/**     → identity-service:8001
/events/**       → event-ingestion:8002
/safety/**       → safety-alert-engine:8003
/notifications/**→ notification-service:8004
/cognitive/**    → cognitive-memory:8005
/daily-care/**   → daily-care:8006
/medical/**      → medical-management:8007
/care-team/**    → care-team:8008
/community/**    → community-social:8009
/ml/**           → ml-prediction-service:8094
/ws/alerts       → safety-alert-engine:8081 (WebSocket)
```

### 2.3 Service Discovery & Configuration

**Eureka Server:**
- Service registration on startup
- Health checks (heartbeat every 30s)
- Client-side load balancing

**Config Server:**
- Native file-backed configuration (classpath:/config-repo)
- Environment-specific profiles (`dev`, `docker`, `prod`)
- Basic Auth security (username: config, password: config123)
- Hot reload capability

**Client Requirements:**
- Dependency: `spring-cloud-starter-config`
- URL format: `http://{username}:{password}@{host}:{port}`
- Profile activation: `SPRING_PROFILES_ACTIVE=docker` for containerized deployment

### 2.4 Security Layer

**Keycloak (Identity Provider):**
- OAuth2/OIDC token issuance
- Role-Based Access Control (RBAC)
- User federation (LDAP/AD support)
- Multi-factor authentication (MFA)

**Roles:**
| Role | Permissions |
|------|-------------|
| `PATIENT` | Read own profile, update preferences |
| `CAREGIVER` | Read assigned patients, daily notes |
| `DOCTOR` | Full medical access, write diagnosis |
| `ADMIN` | Full system access |

**Token Flow:**
```
1. Client → Keycloak: Login request (OAuth2/OIDC)
2. Keycloak → Client: JWT (access + refresh tokens)
3. Client → Gateway: API request + Bearer token
4. Gateway: Validates JWT locally (public key from Keycloak)
5. Gateway → Service: Forward with user context

Note: Services do NOT directly connect to Keycloak for validation.
       Gateway handles all JWT validation.
```

---

## 3. Microservices Layer

### 3.1 Service Interactions

```
                    ┌─────────────────┐
                    │  API Gateway    │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│    Identity   │◄──│     Event     │   │     Safety    │
│    Service    │   │   Ingestion   │   │Alert Engine   │
└───────┬───────┘   └───────┬───────┘   └───────┬───────┘
        │                   │                   │
        │ ┌─────────────────┘                   │
        │ │ Publishes: RawActivityEvent         │ Consumes: RawActivityEvent
        │ └────────────────────────────────────►│
        │                                         │
        │         ┌───────────────────────────────┘ Publishes: AlertCreated
        │         │                               │ NotificationRequested
        │         ▼                               │
        │    ┌────────────┐                       │
        └───►│  RabbitMQ  │◄──────────────────────┘
             └─────┬──────┘
                   │
        ┌──────────┼──────────┐
        │          │          │
        ▼          ▼          ▼
┌────────────┐ ┌────────┐ ┌────────┐
│Notification│ │ Daily  │ │ Care   │
│  Service   │ │  Care  │ │  Team  │
└────────────┘ └────────┘ └────────┘
```

### 3.2 Service Responsibilities

#### Identity Service (Port: 8001)
**Domain:** Patient Profile Management (MS-1)

| Entity | Description | Status |
|--------|-------------|--------|
| PatientProfile | Core clinical data linked to Keycloak ID | ✅ Implemented |
| DoctorProfile | Doctor professional details and contact info | ✅ Implemented |
| CaregiverProfile | Caregiver details and availability | ✅ Implemented |
| AutonomyAssessment | Autonomy levels (INDEPENDENT/ASSISTED/DEPENDENT) across 4 domains | ✅ Implemented |
| Demographics | Extended personal info, emergency contacts *(Planned)* | 📝 Planned |
| RiskFactor | Wandering, falls, medication adherence *(Planned)* | 📝 Planned |
| CarePreferences | Language, cultural context, notifications *(Planned)* | 📝 Planned |

**APIs:**
```
# Patient Profiles
POST   /api/v1/patients                    → Create profile
GET    /api/v1/patients                    → List all profiles
GET    /api/v1/patients/{userId}           → Load profile by Keycloak ID
PUT    /api/v1/patients/{id}               → Update profile

# Doctor/Caregiver Profiles
GET    /api/v1/doctors/{id}                → Get doctor by internal ID
GET    /api/v1/doctors/user/{userId}       → Get doctor by Keycloak ID
GET    /api/v1/caregivers/{id}             → Get caregiver by internal ID
GET    /api/v1/caregivers/user/{userId}    → Get caregiver by Keycloak ID

# Autonomy Assessment
GET    /api/v1/patients/{id}/autonomy           → Get latest autonomy
POST   /api/v1/patients/{id}/autonomy           → New assessment
GET    /api/v1/patients/{id}/autonomy/history   → Get assessment history

# Risk Factors (Planned)
GET    /api/v1/patients/{id}/risk-factors  → List risks (Planned)
POST   /api/v1/patients/{id}/risk-factors  → Add risk factor (Planned)
```

**Admin User Management APIs:**
```
POST   /api/v1/admin/users                  → Create user (auto-creates profile)
GET    /api/v1/admin/users                  → List all users
GET    /api/v1/admin/users/{userId}         → Get user details
PUT    /api/v1/admin/users/{userId}        → Update user
DELETE /api/v1/admin/users/{userId}        → Delete user
POST   /api/v1/admin/users/{userId}/reset-password → Reset password
```

**Admin Profile Management APIs:**
```
GET    /api/v1/admin/profiles/{userId}      → Get any user's profile by role
GET    /api/v1/admin/profiles/{userId}/exists → Check if profile exists
PUT    /api/v1/admin/profiles/{userId}     → Update profile (generic)
PUT    /api/v1/admin/profiles/{userId}/patient   → Update patient profile
PUT    /api/v1/admin/profiles/{userId}/doctor    → Update doctor profile
PUT    /api/v1/admin/profiles/{userId}/caregiver → Update caregiver profile
```

**Profile Auto-Creation:**
When an admin creates a user via `POST /api/v1/admin/users`, the system automatically creates:
- `PatientProfile` for role PATIENT
- `DoctorProfile` for role DOCTOR
- `CaregiverProfile` for role CAREGIVER
- No profile for role ADMIN

**Events Published:** *(Planned)*
- `ProfileUpdated` - Patient data changed (Planned)
- `AutonomyChanged` - New assessment submitted (Planned)

---

#### Event Ingestion Service (Port: 8002)
**Domain:** High-Volume Activity Events

| Entity | Description |
|--------|-------------|
| ActivityEvent | IoT/camera events with timestamp |
| CameraDevice | ESP32 device registration |
| BatchEvent | Grouped events for efficiency |

**APIs:**
```
POST   /api/v1/activity-events             → Submit single event
POST   /api/v1/activity-events/batch       → Submit batch
GET    /api/v1/devices                     → List cameras
POST   /api/v1/devices                     → Register device
```

**Events Published:**
- `RawActivityEvent` → Consumed by Safety Alert Engine

**Characteristics:**
- Partitioned PostgreSQL for horizontal scaling
- 3 instances for high throughput
- Async processing pipeline

---

#### Safety & Alert Engine (Port: 8003)
**Domain:** Risk Detection & Alert Management

| Entity | Description |
|--------|-------------|
| BehaviorLog | Detected patterns from events |
| Alert | Active alert with severity |
| AlertAction | Escalation actions taken |
| RiskRule | Configurable detection rules |

**APIs:**
```
GET    /api/v1/behaviors                   → List behavior logs
GET    /api/v1/alerts                      → List alerts
POST   /api/v1/alerts/{id}/acknowledge     → Acknowledge alert
GET    /api/v1/rules                       → List risk rules
POST   /api/v1/rules                       → Create rule
```

**Events Consumed:**
- `RawActivityEvent` → Pattern detection

**Events Published:**
- `AlertCreated` → New alert generated
- `AlertEscalated` → Severity increased
- `NotificationRequested` → Send notification

**WebSocket:**
- Port 8081 for real-time alert push to clients

---

#### Notification Service (Port: 8004)
**Domain:** Multi-Channel Delivery

| Entity | Description |
|--------|-------------|
| Notification | Delivery record with status |
| Template | Reusable message templates |
| Preference | User channel preferences |

**Channels:**
- SMS (Twilio)
- Email (SendGrid)
- Push (Firebase)
- In-app (WebSocket)

**Events Consumed:**
- `NotificationRequested` → Process delivery
- `AlertCreated` → Urgent alert notification

---

#### Cognitive & Memory Service (Port: 8005)
**Domain:** Brain Training & Memory Aid

| Entity | Description |
|--------|-------------|
| GameActivity | Cognitive exercise sessions |
| Memory | Photo/text memory entries |
| HealthRecord | Aggregated health metrics |

**APIs:**
```
GET    /api/v1/games                       → Available games
POST   /api/v1/games/{id}/sessions        → Record session
GET    /api/v1/memories                   → Patient memories
POST   /api/v1/memories                   → Create memory
GET    /api/v1/health-records             │ Health timeline
```

---

#### Daily Care & Routine Service (Port: 8006)
**Domain:** Habit & Task Management

| Entity | Description |
|--------|-------------|
| Habit | Recurring daily activities |
| HabitTask | Individual task instances |
| Routine | Time-blocked schedule |

**Uses:**
- Identity Service (autonomy level affects task complexity)

---

#### Medical Management Service (Port: 8007)
**Domain:** Healthcare Coordination

| Entity | Description |
|--------|-------------|
| Appointment | Scheduled consultations |
| MedicationPlan | Prescribed medications |
| MedicationIntake | Actual intake tracking |
| Teleconsult | Video session records |

**Features:**
- Jitsi integration for video calls
- MySQL for relational medical data

---

#### Care Team Coordination Service (Port: 8008)
**Domain:** Caregiver/Doctor Assignment

| Entity | Description |
|--------|-------------|
| CaregiverAssignment | Patient-caregiver links |
| DoctorAssignment | Patient-doctor relationships |
| ChecklistItem | Shared task lists |
| Handover | Shift transition notes |

---

#### Community & Social Service (Port: 8009)
**Domain:** Social Platform

| Entity | Description |
|--------|-------------|
| Post | Forum discussions |
| Comment | Thread replies |
| Activity | Social events |
| Registration | Event sign-ups |

---

#### ML Prediction Service (Port: 8094)
**Domain:** AI/ML Analytics

| Function | Description |
|----------|-------------|
| Risk Prediction | Predict fall/wandering probability |
| Anomaly Detection | Unusual behavior patterns |
| Sentiment Analysis | NLP on patient text |

**Integration:**
- Consumes historical data from Identity/Event services via REST
- Python-based (Flask/FastAPI)
- **No database** (stateless predictions)
- Calls External APIs for ML model serving (if applicable)

---

## 4. Data Layer

### 4.1 Database Strategy

| Service | Database | Reason |
|---------|----------|--------|
| Identity | PostgreSQL | Complex relational data, JSONB for flexibility |
| Event | PostgreSQL | Partitioning for time-series events |
| Safety | PostgreSQL | ACID for alert consistency |
| Cognitive | PostgreSQL | Relational game/memory data |
| Daily Care | PostgreSQL | Routine/habit relationships |
| Notification | MongoDB | Flexible document schema for templates |
| Medical | MySQL | Legacy compatibility, medical records |
| Care Team | MySQL | Relational assignments |
| Community | MySQL | Social graph data |

### 4.2 Data Consistency

**Strong Consistency:**
- Patient profile updates
- Medical records
- Alert state changes

**Eventual Consistency:**
- Notification delivery status
- Analytics aggregations
- ML feature updates

---

## 5. Communication Patterns

### 5.1 Synchronous (REST)
- Client → Gateway → Service
- Service → Service (avoided where possible)

### 5.2 Asynchronous (RabbitMQ)

**Exchanges:**
```
activity.events        → safety.alert.engine
alert.notifications    → notification.service
profile.updates        → all.interested.services
```

**Event Schema:**
```json
{
  "eventId": "uuid",
  "eventType": "AlertCreated",
  "timestamp": "2026-02-12T10:30:00Z",
  "source": "safety-alert-engine",
  "payload": {
    "patientId": "uuid",
    "alertType": "WANDERING",
    "severity": "HIGH"
  }
}
```

### 5.3 Real-Time Notifications
- Safety Alert Engine publishes to RabbitMQ
- Notification Service consumes and triggers:
  - SMS (Twilio)
  - Email (SendGrid)
  - Push (Firebase)
  - In-app notifications

---

## 6. Security Architecture

### 6.1 Authentication Flow

**Account Creation (Pre-Login):**
```
┌─────────┐     ┌─────────────┐     ┌─────────┐     ┌──────────┐
│ Doctor  │────►│ Identity MS │────►│ Keycloak│────►│  Admin   │
│ /Admin  │     │ (create)    │     │ (user)  │     │ (doctor  │
└─────────┘     └─────────────┘     └─────────┘     │ approval)│
     │                                              └──────────┘
     │ 1. Create patient/caregiver
     │    (no validation needed)
     │
     │◄──────────────────────────────────────────────────────┐
     │ 2. Send temp credentials (email/SMS)                  │
     │                                                       │
     │    OR for doctor self-signup:                         │
     │    Doctor ──► Signup ──► Pending ──► Admin validates  │
     └───────────────────────────────────────────────────────┘
```

**Login Flow:**
```
┌─────────┐     ┌──────────┐     ┌─────────┐     ┌──────────┐
│ Client  │────►│ Keycloak │────►│ Gateway │────►│ Service  │
└─────────┘     └──────────┘     └─────────┘     └──────────┘
     │                │                │               │
     │ 1. Login       │                │               │
     │───────────────►│                │               │
     │                │                │               │
     │ 2. JWT Tokens  │                │               │
     │◄───────────────│                │               │
     │    (if first   │                │               │
     │     login:     │                │               │
     │     force pwd  │                │               │
     │     change)    │                │               │
     │                │                │               │
     │ 3. API + JWT   │                │               │
     │────────────────┼───────────────►│               │
     │                │                │               │
     │                │ 4. Validate    │               │
     │                │◄───────────────│               │
     │                │                │               │
     │                │ 5. Forward     │               │
     │                │                │──────────────►│
```

### 6.2 Authorization Matrix

| Endpoint | PATIENT | CAREGIVER | DOCTOR | ADMIN |
|----------|---------|-----------|--------|-------|
| GET /patients/{id} | Own only | Assigned | Assigned | All |
| PUT /patients/{id} | Own prefs | Daily notes | Medical fields | All |
| POST /risk-factors | ❌ | ❌ | ✅ | ✅ |
| GET /alerts | Own only | Assigned | Assigned | All |
| POST /appointments | ❌ | ❌ | ✅ | ✅ |

---

## 7. External Integrations

| Service | Provider | Purpose |
|---------|----------|---------|
| Image Storage | Cloudinary | Patient photos, memory images |
| SMS | Twilio | Alert notifications |
| Email | SendGrid | Transactional emails |
| Push | Firebase | Mobile notifications |
| Video | Jitsi | Teleconsultation |

---

## 8. Deployment Contexts

### 8.1 Development (Docker Desktop)
- Single host
- Docker Compose
- Hot reload enabled
- H2/embedded databases optional

### 8.2 Production (Kubernetes)
- Multi-zone cluster
- Helm charts
- HPA autoscaling
- Database replication
- Monitoring stack

---

*AlzCare Platform | Logical Architecture Reference | v1.0*
