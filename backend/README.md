# AlzCare – Backend

Spring Boot 3.2 / Spring Cloud microservices for the AlzCare platform. Part of the [AlzCare monorepo](../README.md).

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


## Running locally

```bash
cp .env.example .env
# Start infrastructure first
docker compose up -d eureka-server config-server rabbitmq postgres-keycloak keycloak
# Then the application services
docker compose up -d
```

Services that are referenced by the gateway but not part of this repository
(`safety-alert-engine`, `event-ingestion`, `community-social-service`) are behind the
`full` compose profile: `docker compose --profile full up -d`.

### Access Points
| Service | URL | Credentials |
|---------|-----|-------------|
| Web Application | http://localhost:4200 | - |
| API Gateway | http://localhost:8080 | JWT Token |
| Eureka Dashboard | http://localhost:8761 | - |
| RabbitMQ Management | http://localhost:15672 | see docker-compose.yml |
| Keycloak Admin | http://localhost:8090 | see docker-compose.yml (dev only) |

---

