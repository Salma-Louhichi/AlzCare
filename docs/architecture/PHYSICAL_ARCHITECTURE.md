# ALZCARE PLATFORM - PHYSICAL ARCHITECTURE

> Docker Desktop Development Environment | v1.0 | Feb 2026

## Overview

This document describes the container layout, network topology, and service configurations for local development using Docker Desktop.

### Resource Summary
| Metric | Value |
|--------|-------|
| Total Containers | 27 |
| CPU Allocation | 15.5 cores / 28 cores |
| RAM Allocation | 12.5 GB / 16 GB |
| Networks | 3 (frontend, backend, database) |
| Default Scale | 1 instance each |

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PHYSICAL ARCHITECTURE                             │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │ Frontend-network │  │  Backend-network │  │ Database-network │          │
│  │   (Port 4200+)   │  │   (Port 8000+)   │  │   (Port 3306+)   │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Network Topology

| Network | Purpose | Connected Services |
|---------|---------|-------------------|
| `frontend-network` | Client-facing layer | Angular apps, Gateway |
| `backend-network` | Service communication | Gateway, Infrastructure, All Microservices |
| `database-network` | Data persistence | All Databases (isolated from frontend) |

### Network Rules
- **Frontend** can only reach `gateway-service` (port 8080)
- **Backend** services communicate via Eureka + RabbitMQ
- **Database** network is isolated - only backend services with `+ db` access can connect

---

## 1. Frontend Layer

### 1.1 angular-webapp
| Property | Value |
|----------|-------|
| Container | `webapp` |
| Port | `4200` → `80` |
| Image | `alzcare/webapp` |
| Network | `frontend-network` |
| Description | Unified Angular UI for all roles (Patient/Caregiver/Doctor/Admin) |

---

## 2. Gateway & Infrastructure Layer

### 2.1 gateway-service
| Property | Value |
|----------|-------|
| Port | `8080` |
| Image | `alzcare/gateway` |
| Networks | `backend-network` + `frontend-network` |
| Description | Spring Cloud Gateway - Routes all requests, JWT validation, rate limiting |
| Key Routes | `/api/v1/**` → Microservices, WebSocket proxy |

### 2.2 eureka-server
| Property | Value |
|----------|-------|
| Port | `8761` |
| Image | `springcloud/eureka` |
| Network | `backend-network` |
| Description | Service Discovery Registry |
| URL | http://localhost:8761 |

### 2.3 config-server
| Property | Value |
|----------|-------|
| Port | `8888` |
| Image | `alzcare/config` |
| Network | `backend-network` |
| Description | Centralized Configuration (Native file backend) |
| Credentials | `config` / `config123` |

**Client Configuration:**
Services must include credentials in the Config Server URL:
```yaml
SPRING_CONFIG_IMPORT: "optional:configserver:http://config:config123@config-server:8888"
```

**Required Dependency:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

### 2.4 rabbitmq
| Property | Value |
|----------|-------|
| Port | `5672` (AMQP), `15672` (Management) |
| Image | `rabbitmq:3` |
| Network | `backend-network` |
| Description | Message Broker for async communication |
| Management UI | http://localhost:15672 (guest/guest) |

### 2.5 KEYCLOAK
| Property | Value |
|----------|-------|
| Port | `8090` |
| Image | `keycloak:22` |
| Networks | `backend-network` + `database-network` |
| Description | OAuth2/OIDC Identity Provider |
| Features | JWT tokens, Role management (PATIENT, CAREGIVER, DOCTOR, ADMIN) |
| Database | `postgres-keycloak` (port 5439) |

---

## 3. Microservices Layer

### Service Entities Overview

| Service | Entities | Database |
|---------|----------|----------|
| **identity-service** | PatientProfile, DoctorProfile, CaregiverProfile, AutonomyAssessment | PostgreSQL |
| **event-ingestion** | CameraDevice, ActivityEvent | PostgreSQL |
| **safety-alert-engine** | BehaviorLog, Alert, AlertAction, RiskRule | PostgreSQL |
| **notification-service** | Notification | MongoDB |
| **cognitive-memory** | GameActivity, Memory, HealthRecord | PostgreSQL |
| **daily-care** | Habit, HabitTask | PostgreSQL |
| **medical-management** | Appointment, MedicationPlan, MedicationItem, MedicationIntake | MySQL |
| **care-team** | ChecklistItem, CaregiverAssignment, DoctorAssignment, CaregiverHandover | MySQL |
| **community-social** | Post, Comment, Activity, Registration | MySQL |
| **ml-prediction-service** | *(stateless - no entities)* | *(none)* |

### 3.1 identity-service (Patient Profile MS)
| Property | Value |
|----------|-------|
| Port | `8001` |
| Networks | `backend-network` + `database-network` |
| Resources | CPU: 0.5, RAM: 512M |
| Database | `postgres-identity` (port 5432) |
| Description | Patient/Doctor/Caregiver profiles, Autonomy assessments, Care team assignments |

**Entities:**
| Entity | Key Attributes | Status |
|--------|----------------|--------|
| **PatientProfile** | id, userId, firstName, lastName, dateOfBirth, gender, preferredLanguage, photoUrl, phone, emergencyContact, address, isActive, totalPoints, currentStreak | ✅ Implemented |
| **DoctorProfile** | id, userId, firstName, lastName, photoUrl, speciality, licenseNumber, phone, contact, address, isAvailable | ✅ Implemented |
| **CaregiverProfile** | id, userId, firstName, lastName, photoUrl, phone, address, contact, isAvailable, isProfessional | ✅ Implemented |
| **AutonomyAssessment** | id, patientId, hygieneLevel, medicationLevel, mobilityLevel, feedingLevel, overallScore, assessedBy, assessmentDate | ✅ Implemented |

**Enums:**
| Enum | Values |
|------|--------|
| **GenderEnum** | MALE, FEMALE |
| **LanguageEnum** | ARABIC, FRENCH, ENGLISH |
| **AutonomyLevel** | INDEPENDENT, ASSISTED, DEPENDENT |

### 3.2 event-ingestion
| Property | Value |
|----------|-------|
| Port | `8002` |
| Networks | `backend-network` + `database-network` |
| Resources | CPU: 1.0, RAM: 1G |
| Scale | 3 instances (high throughput) |
| Database | `postgres-event` (port 5433) |
| Description | High-throughput HTTP ingestion, Device management, ESP32 camera events |

**Entities:**
| Entity | Key Attributes |
|--------|----------------|
| **CameraDevice** | id, MAC, patientId, zone, status, ipAddress, lastSeen, pairedAt, configuration |
| **ActivityEvent** | id, patientId, deviceId, timestamp, motionIntensity, duration, zone, context, processedForBehavior |

**Enums:**
| Enum | Values |
|------|--------|
| **CameraStatus** | ACTIVE, OFFLINE, PAUSED |
| **CameraZone** | BEDROOM, HALLWAY, BATHROOM, FRONT_DOOR |

### 3.3 safety-alert-engine
| Property | Value |
|----------|-------|
| Port | `8003` |
| Networks | `backend-network` + `database-network` |
| Resources | CPU: 0.5, RAM: 512M |
| Database | `postgres-safety` (port 5434) |
| Description | Pattern detection, Risk rule evaluation, Alert lifecycle, Escalation workflow |
| Note | Publishes alerts to RabbitMQ for async notification delivery |

**Entities:**
| Entity | Key Attributes |
|--------|----------------|
| **BehaviorLog** | id, patientId, type, severity, timestamp, location, triggers, witnesses, source, validationStatus, imageUrls |
| **Alert** | id, patientId, ruleCode, severity, status, message, currentLevel, autoEscalated, triggeredAt, resolvedAt |
| **RiskRule** | ruleCode, name, description, severity, thresholds, isActive |
| **AlertAction** | id, alertId, actionType, targetRole, channel, sentAt, deadlineAt, resolutionAction |

**Enums:**
| Enum | Values |
|------|--------|
| **BehaviorType** | FALL, WANDERING, AGITATION, SLEEP_DISORDER, HALLUCINATION, CONFUSION, AGGRESSION, MEDICATION_REFUSAL |
| **BehaviorSeverity** | 1, 2, 3, 4, 5 |
| **BehaviorSource** | MANUAL, AUTO |
| **BehaviorValidationStatus** | PENDING, CONFIRMED, FALSE_ALARM |
| **AlertStatus** | ACTIVE, RESOLVED |
| **AlertSeverity** | LOW, MEDIUM, HIGH, CRITICAL |
| **RiskRuleCode** | FALL_2_IN_48H, FALL_SEVERITY_CRITICAL, NIGHT_MOTION, NIGHT_MOTION_SEQUENCE, PROLONGED_INACTIVITY, RESTLESSNESS_SPIKE, WANDERING_DURATION |
| **EscalationLevel** | CAREGIVER, DOCTOR, EMERGENCY_CONTACT |
| **NotificationChannel** | APP_PUSH, SMS |
| **AlertActionType** | NOTIFIED, ESCALATED, ACKNOWLEDGED, RESOLVED, FALSE_POSITIVE |

### 3.4 notification-service
| Property | Value |
|----------|-------|
| Port | `8004` |
| Networks | `backend-network` + `database-network` |
| Resources | CPU: 0.5, RAM: 512M |
| Database | `mongo-notification` (port 27017) |
| Description | Multi-channel delivery (SMS/Email/Push), Template rendering, Retry logic |
| External APIs | Cloudinary (media), Twilio (SMS), SendGrid (Email), Firebase (Push) |

**Entities:**
| Entity | Key Attributes |
|--------|----------------|
| **Notification** | id, recipientId, type, channel, status, title, message, sentAt, deliveredAt, readAt, retryCount |
| **NotificationTemplate** | id, name, type, subject, content, variables, language |

### 3.5 cognitive-memory
| Property | Value |
|----------|-------|
| Port | `8005` |
| Networks | `backend-network` + `database-network` |
| Resources | CPU: 0.5, RAM: 512M |
| Database | `postgres-cognitive` (port 5435) |
| Description | Cognitive games, Memory wallet, Health record aggregation |

### 3.6 daily-care
| Property | Value |
|----------|-------|
| Port | `8006` |
| Networks | `backend-network` + `database-network` |
| Resources | CPU: 0.5, RAM: 512M |
| Scale | 1-2 instances |
| Database | `postgres-daily` (port 5436) |
| Description | Daily routine management, Task tracking, Completion workflow |

**Entities:**
| Entity | Key Attributes |
|--------|----------------|
| **Habit** | id, patientId, name, type, targetTime, isActive, createdAt |
| **HabitTask** | id, habitId, title, description, orderIndex, isCritical, autonomyMode |
| **Routine** | id, patientId, name, dayOfWeek, tasks, isActive |

**Enums:**
| Enum | Values |
|------|--------|
| **HabitType** | MORNING, EVENING, ACTIVITY |
| **AutonomyMode** | INDEPENDENT, ASSISTED, DEPENDENT |

### 3.7 medical-management
| Property | Value |
|----------|-------|
| Port | `8007` |
| Networks | `backend-network` + `database-network` |
| Resources | CPU: 0.5, RAM: 512M |
| Database | `mysql-medical` (port 3306) |
| Description | Appointment scheduling, Medication plans, Intake tracking, Teleconsultation |
| External API | Jitsi (video conferencing) |

**Entities:**
| Entity | Key Attributes |
|--------|----------------|
| **Appointment** | id, patientId, doctorId, type, priority, mode, status, startAt, endAt, attendanceStatus, meetingUrl |
| **MedicationPlan** | id, patientId, doctorId, title, startDate, endDate, autonomyLevel, status, version, lastRiskLevel |
| **MedicationItem** | id, planId, name, dosage, frequency, timesOfDay, isHighRisk, stockQuantity, expirationDate |
| **MedicationIntake** | id, itemId, scheduledAt, status, confirmedByRole |

**Enums:**
| Enum | Values |
|------|--------|
| **AppointmentStatus** | REQUESTED, ACCEPTED, REJECTED, CONFIRMED, COMPLETED, CANCELLED |
| **AppointmentMode** | ONSITE, ONLINE |
| **AppointmentType** | ROUTINE, FOLLOW_UP, COGNITIVE_TEST, EMERGENCY |
| **AppointmentPriority** | LOW, NORMAL, HIGH, CRITICAL |
| **FrequencyType** | DAILY, WEEKLY, CUSTOM |
| **IntakeStatus** | PENDING, TAKEN, DELAYED, MISSED, REFUSED |
| **PlanStatus** | ACTIVE, SUSPENDED, STOPPED |
| **RiskLevel** | LOW, MEDIUM, HIGH |

### 3.8 care-team
| Property | Value |
|----------|-------|
| Port | `8008` |
| Networks | `backend-network` + `database-network` |
| Resources | CPU: 0.5, RAM: 512M |
| Scale | 1-2 instances |
| Database | `mysql-careteam` (port 3307) |
| Description | Task assignment, Caregiver/Doctor assignments, Shift handovers |

**Entities:**
| Entity | Key Attributes |
|--------|----------------|
| **ChecklistItem** | id, doctorId, patientId, date, itemOrder, description, assignedCaregiverId, completedAt, completedBy |
| **CaregiverHandover** | id, patientId, fromCaregiverId, toCaregiverId, notes, acknowledged, acknowledgedAt |
| **DoctorAssignment** | id, doctorId, patientId, assignedAt, rejectionReason |
| **CaregiverAssignment** | id, caregiverId, patientId, assignedAt, revokedAt, unavailableFrom, unavailableTo |

**Enums:**
| Enum | Values |
|------|--------|
| **AssignmentStatus** | PENDING, ASSIGNED, COMPLETED |
| **HandoverStatus** | PENDING, ACKNOWLEDGED |

### 3.9 community-social
| Property | Value |
|----------|-------|
| Port | `8009` |
| Networks | `backend-network` + `database-network` |
| Resources | CPU: 0.5, RAM: 512M |
| Scale | 1-2 instances |
| Database | `mysql-community` (port 3308) |
| Description | Forum discussions, Social activities, Event registration |

**Entities:**
| Entity | Key Attributes |
|--------|----------------|
| **Post** | id, title, content, category, parentId, createdBy, creationDate, likeCount, popularityScore |
| **Comment** | id, content, creationDate, userId, postId |
| **Activity** | id, title, description, type, startDate, endDate, location, latitude, longitude, maxCapacity, registeredCount, status |
| **Registration** | id, activityId, patientId, caregiverId, status, registrationDate, specialNeeds |

**Enums:**
| Enum | Values |
|------|--------|
| **DiscussionCategory** | ADVICE, SUPPORT, RESOURCES, SUCCESS_STORIES, QUESTIONS |
| **ActivityType** | GROUP, INDIVIDUAL, VIRTUAL, IN_PERSON |
| **ActivityStatus** | PUBLISHED, IN_PROGRESS, CANCELLED |
| **RegistrationStatus** | PENDING, CONFIRMED, RECORDED, CANCELLED |

### 3.10 ml-prediction-service
| Property | Value |
|----------|-------|
| Port | `8094` |
| Networks | `backend-network` |
| Resources | CPU: 1.0, RAM: 2G |
| Image | `alzcare/ml-api` |
| Scale | 1-2 instances |
| Description | Risk prediction models, Anomaly detection, NLP sentiment analysis |
| Note | Python-based (Flask/FastAPI), no dedicated DB |

---

## 4. Database Layer

### 4.1 PostgreSQL Databases

| Service | Container | Port | Volume | Image |
|---------|-----------|------|--------|-------|
| Keycloak | `postgres-keycloak` | `5439` | `pg_keycloak` | `postgres:15` |
| Identity | `postgres-identity` | `5432` | `pg_identity` | `postgres:15` |
| Event | `postgres-event` | `5433` | `pg_event` | `postgres:15` |
| Safety | `postgres-safety` | `5434` | `pg_safety` | `postgres:15` |
| Cognitive | `postgres-cognitive` | `5435` | `pg_cognitive` | `postgres:15` |
| Daily Care | `postgres-daily` | `5436` | `pg_daily` | `postgres:15` |

### 4.2 MySQL Databases

| Service | Container | Port | Volume | Image |
|---------|-----------|------|--------|-------|
| Medical | `mysql-medical` | `3306` | `mysql_medical` | `mysql:8` |
| Care Team | `mysql-careteam` | `3307` | `mysql_care` | `mysql:8` |
| Community | `mysql-community` | `3308` | `mysql_comm` | `mysql:8` |

### 4.3 MongoDB

| Service | Container | Port | Volume | Image |
|---------|-----------|------|--------|-------|
| Notifications | `mongo-notification` | `27017` | `mongo_notif` | `mongo:6` |

---

## 5. External Services (SaaS)

These are not containerized but used by the platform:

| Service | Purpose | Used By |
|---------|---------|---------|
| **Cloudinary** | Image/video storage | notification-service, identity-service (patient photos) |
| **Twilio** | SMS gateway | notification-service |
| **SendGrid** | Email gateway | notification-service |
| **Firebase** | Push notifications | notification-service (mobile) |
| **Jitsi** | Video conferencing | medical-management (teleconsultation) |

---

## 6. Port Reference

### Frontend
| Port | Service |
|------|---------|
| 4200 | angular-webapp (all roles) |

### Gateway & Infrastructure
| Port | Service |
|------|---------|
| 8080 | gateway-service |
| 8761 | eureka-server |
| 8888 | config-server |
| 5672 | rabbitmq (AMQP) |
| 15672 | rabbitmq (Management UI) |
| 8090 | keycloak |
| 8081 | safety-alert-engine (WebSocket) |

### Microservices
| Port | Service |
|------|---------|
| 8001 | identity-service |
| 8002 | event-ingestion |
| 8003 | safety-alert-engine |
| 8004 | notification-service |
| 8005 | cognitive-memory |
| 8006 | daily-care |
| 8007 | medical-management |
| 8008 | care-team |
| 8009 | community-social |
| 8094 | ml-prediction-service |

### Databases
| Port | Service |
|------|---------|
| 5432 | postgres-identity |
| 5433 | postgres-event |
| 5434 | postgres-safety |
| 5435 | postgres-cognitive |
| 5436 | postgres-daily |
| 5439 | postgres-keycloak |
| 3306 | mysql-medical |
| 3307 | mysql-careteam |
| 3308 | mysql-community |
| 27017 | mongo-notification |

---

## 7. Development Quick Reference

### Start All Services
```bash
docker-compose up -d
```

### Access Points
- **Web App**: http://localhost:4200
- **API Gateway**: http://localhost:8080
- **Eureka**: http://localhost:8761
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **Keycloak**: http://localhost:8090

### Service Dependencies
```
All Microservices depend on:
  ├─ Eureka Server (registration)
  ├─ Config Server (bootstrap config)
  ├─ RabbitMQ (message bus)
  └─ Keycloak (JWT validation)

Databases are isolated:
  └─ Only accessible via database-network
```

### Health Check Order
1. Start: `postgres-keycloak`, `eureka-server`, `rabbitmq`
2. Start: `keycloak`, `config-server`
3. Start: All microservices
4. Start: `gateway-service`
5. Start: Frontend apps

---

## 8. Production Considerations

This architecture is for **Docker Desktop development**. Production differs:

| Aspect | Dev (Current) | Production |
|--------|--------------|------------|
| Orchestration | Docker Compose | Kubernetes (EKS/GKE/AKS) |
| Scaling | Manual | HPA (2-10 replicas) |
| Databases | Single container | Primary-Replica clusters |
| Keycloak | Single instance | 2+ replicas |
| Networking | Docker networks | K8s namespaces + Ingress |
| Secrets | Env vars | Kubernetes Secrets/Vault |
| Monitoring | None | Prometheus + Grafana + ELK |

---

*AlzCare Platform | Physical Architecture Reference | Docker Desktop v1.0*
