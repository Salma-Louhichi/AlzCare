# AlzCare – Alzheimer's Disease Management Platform

AlzCare is a web platform for Alzheimer's disease management that supports **patients, caregivers, doctors and administrators** through a Spring Boot microservices backend and an Angular frontend.

> **Maintainer:** Salma Louhichi — [@Salma-Louhichi](https://github.com/Salma-Louhichi)

---

## Repository layout

```
AlzCare/
├── backend/                 # Spring Boot 3 / Spring Cloud microservices + docker-compose
│   ├── eureka-server/       # Service discovery (8761)
│   ├── config-server/       # Centralised configuration (8888)
│   ├── gateway-service/     # API gateway + JWT validation (8080)
│   ├── identity-service/    # Users & profiles, Keycloak admin integration (8001)
│   ├── notification-service/# Notifications, schedules, RabbitMQ consumer (8004)
│   ├── care-team-service/   # Care-team assignments, handovers, checklists (8008)
│   └── keycloak-service/    # Keycloak realm configuration (8090)
├── frontend/                # Angular 18 standalone app + Tailwind CSS (4200)
├── docs/                    # Architecture documentation
└── IMPROVEMENTS.md          # Engineering review & improvement roadmap
```

## Features

### Patient Features
- **Health Metrics Dashboard** – Track vitals, mood, and wellness indicators
- **Medication Management** – Organized prescription schedules and reminders
- **Daily Activities** – Task tracking and activity logs
- **Brain Games** – Cognitive exercises for mental stimulation
- **Community Forum** – Discussion platform with posts, comments, likes, and categories (Advice, Support, Resources, Success Stories, Questions)
- **Profile Management** – Personal health information and care preferences

### Caregiver Features
- **Patient Overview Dashboard** – Quick access to assigned patients and key metrics
- **Behavior Tracking** – Log and monitor patient behaviors with image upload (drag-drop, gallery, camera)
- **Behavior History** – Review logs with photo thumbnails and full-screen lightbox viewer
- **Quick Actions** – Direct "Log Behavior" button with patient pre-selection

### Doctor Features
- **Patient Cases Overview** – Dashboard for monitoring assigned patients
- **Care Coordination** – Interface for managing patient care plans

### Admin Features
- **System Dashboard** – Overview of platform metrics and activity
- **User Management** – CRUD operations for all user roles (Patient, Caregiver, Doctor, Admin)
- **Medical Records Access** – View and manage patient medical information
- **Community Moderation** – Moderate forum posts and comments
- **Notification Schedule Management** – Create and manage scheduled notification campaigns
- **Analytics** – Monitor system-wide metrics and usage statistics
- **Settings** – System configuration and parameters

### Shared Features
- **Real-time Notifications** – Notification bell with polling, toast alerts, and dedicated notification page
- **Safety Alerts** – Safety alert management system with risk detection
- **Landing Page** – Public-facing introduction to the platform
- **Unified Dashboard Entry** – `/dashboard` route auto-redirects to role-appropriate interface
- **Automatic Token Refresh** – Seamless authentication with Keycloak OAuth2 (no session timeouts)

---

## Tech Stack

### Frontend
- **Framework**: Angular 18+ (Standalone Components)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Reactive Programming**: RxJS
- **Authentication**: Keycloak OAuth2 / OpenID Connect
- **Build Tool**: Angular CLI
- **Image Handling**: Cloudinary integration for uploads

### Backend
- **Language**: Java 17
- **Framework**: Spring Boot 3.2
- **API Gateway**: Spring Cloud Gateway (Port 8080)
- **Service Discovery**: Netflix Eureka (Port 8761)
- **Configuration**: Spring Cloud Config Server (Port 8888)
- **Authentication**: Keycloak 22 (Port 8090)
- **Message Broker**: RabbitMQ 3 (Port 5672 / 15672)

### Databases
- **PostgreSQL 15** – Identity, Event Ingestion, Safety Alert, Cognitive, Daily Care services
- **MySQL 8** – Care Team, Community Social, Medical Management services
- **MongoDB 6** – Notification Service (flexible document schema)

### DevOps & Infrastructure
- **Containerization**: Docker & Docker Compose
- **Version Control**: Git
- **API Communication**: OpenFeign (synchronous), RabbitMQ (asynchronous)

---

## Architecture

AlzCare follows a **Layered Microservices Architecture** with 10 microservices:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              CLIENT LAYER                                │
│                    Angular Web App (Port 4200)                          │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           GATEWAY LAYER                                  │
│              Spring Cloud Gateway (Port 8080)                           │
│         (Routing, Load Balancing, Authentication)                       │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         SERVICE LAYER (Microservices)                    │
├─────────────────┬─────────────────┬─────────────────┬───────────────────┤
│  Identity       │  Event          │  Safety Alert   │  Notification     │
│  Service        │  Ingestion      │  Engine         │  Service          │
│  (Port 8001)    │  (Port 8002)    │  (Port 8003)    │  (Port 8004)      │
├─────────────────┼─────────────────┼─────────────────┼───────────────────┤
│  Cognitive      │  Daily Care     │  Medical        │  Care Team        │
│  Memory         │  (Planned)      │  Management     │  Service          │
│  (Port 8005)    │  (Port 8006)    │  (Port 8007)    │  (Port 8008)      │
├─────────────────┴─────────────────┴─────────────────┴───────────────────┤
│                    Community Social Service (Port 8009)                  │
│                    ML Prediction Service (Port 8094)                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        INFRASTRUCTURE LAYER                              │
├─────────────────┬─────────────────┬─────────────────┬───────────────────┤
│  Eureka Server  │  Config Server  │    Keycloak     │     RabbitMQ      │
│  (Port 8761)    │  (Port 8888)    │  (Port 8090)    │  (Port 5672)      │
└─────────────────┴─────────────────┴─────────────────┴───────────────────┘
```

### Communication Patterns
- **Synchronous**: REST via Gateway (Client → Gateway → Service), OpenFeign for inter-service calls
- **Asynchronous**: RabbitMQ for event-driven communication (Safety Alert → Notification)

### Authentication Flow
```
[User Login] → Keycloak (auth) → JWT Token → API Gateway → Microservices
                                      ↓
                               Service Discovery (Eureka)
                                      ↓
                              Async Events (RabbitMQ)
```

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

See [backend/README.md](backend/README.md) and [frontend/README.md](frontend/README.md) for details.

---

## Author

**Salma Louhichi** — [@Salma-Louhichi](https://github.com/Salma-Louhichi)

AlzCare originated as a 4th-year engineering project (PIDEV 4SAE, 2025–2026) at Esprit School of Engineering, Tunisia.
