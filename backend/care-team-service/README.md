# Care Team Service

Caregiver and Doctor Assignment management service for AlzCare Platform. Handles Axis 5 (Care Coordination) and Axis 6 (Care Planning) of the AlzCare system.

## Port
- 8008

## Database
- MySQL (port 3307)

## Features
- **Caregiver Assignment** - Invite-based caregiver linking with role management (Primary, Family, Emergency)
- **Doctor Assignment** - Assign doctors to patients with status tracking
- **Checklist Management** - Daily task checklists for doctors to assign and caregivers to complete
- **Handover Notes** - Transfer care information between caregivers with acknowledgment
- **Availability Tracking** - Mark caregivers as temporarily unavailable with reason
- **Invite System** - Token-based secure invitation flow for caregiver onboarding

---

## ID Types

This service uses UUID-based identifiers to align with the identity-service (Keycloak):

- **Entity IDs**: Auto-generated UUIDs for care-team entities (assignments, handovers, checklist items)
- **User IDs**: String UUIDs referencing users in Keycloak (doctorId, patientId, caregiverId)

### Example API Request
```json
POST /api/v1/care-team/doctors/550e8400-e29b-41d4-a716-446655440000/patients/create
{
  "patientId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "notes": "Initial assignment"
}
```

### Migration Note for Developers

When upgrading from Long-based IDs to UUID:

1. **Database Schema**: Entity ID columns have been migrated from `BIGINT` to `UUID` type
2. **API Changes**: All endpoints now accept and return UUID strings instead of numeric IDs
3. **User References**: The `doctorId`, `patientId`, and `caregiverId` fields are String UUIDs from Keycloak
4. **Entity IDs**: The `id` fields of assignments, handovers, and checklist items are now UUID type
5. **Frontend Updates**: Update all API calls to pass UUID strings instead of Long values

---

## API Endpoints

### Caregiver Assignment Endpoints
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/caregivers/generate-invite` | Generate invite link for caregiver | ✅ Implemented |
| GET | `/invitations/{token}/validate` | Validate invite token | ✅ Implemented |
| POST | `/invitations/{token}/accept` | Accept invite and link caregiver | ✅ Implemented |
| GET | `/patients/{patientId}/caregivers` | List all caregivers for a patient | ✅ Implemented |
| GET | `/caregivers/{caregiverId}/assignments` | List all assignments for a caregiver | ✅ Implemented |
| PUT | `/caregivers/assignments/{id}/role` | Change caregiver's role | ✅ Implemented |
| DELETE | `/caregivers/assignments/{id}` | Revoke caregiver access | ✅ Implemented |
| PUT | `/caregivers/assignments/{id}/availability` | Mark caregiver unavailable | ✅ Implemented |

### Doctor Assignment Endpoints
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/doctors/{doctorId}/patients/create` | Assign doctor to patient | ✅ Implemented |
| GET | `/doctors/{doctorId}/patients` | List all patients assigned to doctor | ✅ Implemented |
| GET | `/patients/{patientId}/doctor` | Get active doctor for a patient | ✅ Implemented |
| PUT | `/doctor-assignments/{id}/deactivate` | Deactivate doctor assignment | ✅ Implemented |

### Checklist Item Endpoints
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/checklists/items` | Create checklist item | ✅ Implemented |
| GET | `/checklists/items/{id}` | Get checklist item by ID | ✅ Implemented |
| GET | `/checklists/doctor/{doctorId}` | Get all items for a doctor | ✅ Implemented |
| GET | `/checklists/patient/{patientId}/date/{date}` | Get items for patient on date | ✅ Implemented |
| PUT | `/checklists/items/{id}/complete` | Mark item as completed | ✅ Implemented |
| PUT | `/checklists/items/{id}/assign` | Assign item to caregiver | ✅ Implemented |
| DELETE | `/checklists/items/{id}` | Delete checklist item | ✅ Implemented |

### Caregiver Handover Endpoints
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/caregivers/handover` | Create handover note | ✅ Implemented |
| PUT | `/caregivers/handover/{id}/acknowledge` | Acknowledge handover | ✅ Implemented |
| GET | `/caregivers/patients/{patientId}/handovers` | Get handovers for patient | ✅ Implemented |

### Actuator
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check endpoint |
| GET | `/actuator/info` | Service information |

---

## Entity Documentation

### CaregiverAssignment Entity

Manages the relationship between caregivers and patients with role-based access control.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key (auto-generated) |
| `caregiverId` | String | Reference to caregiver user (Keycloak UUID) |
| `patientId` | String | Reference to patient user (Keycloak UUID) |
| `role` | CaregiverRole | PRIMARY, FAMILY, or EMERGENCY |
| `status` | AssignmentStatus | PENDING, ACTIVE, or REVOKED |
| `assignedAt` | LocalDateTime | When assignment was created |
| `revokedAt` | LocalDateTime | When access was revoked (if applicable) |
| `inviteToken` | String | Secure token for invitation flow |
| `inviteExpiresAt` | LocalDateTime | Token expiration timestamp |
| `inviteAcceptedAt` | LocalDateTime | When invite was accepted |
| `unavailableFrom` | LocalDateTime | Start of unavailability period |
| `unavailableTo` | LocalDateTime | End of unavailability period |
| `unavailabilityReason` | String | Reason for temporary unavailability |

**Note:** The invite flow ensures secure caregiver onboarding:
1. Doctor/Admin generates invite token
2. Caregiver receives token via secure channel
3. Caregiver validates and accepts the invite
4. Assignment status changes from PENDING to ACTIVE

### DoctorAssignment Entity

Manages the relationship between doctors and patients.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key (auto-generated) |
| `doctorId` | String | Reference to doctor user (Keycloak UUID) |
| `patientId` | String | Reference to patient user (Keycloak UUID) |
| `createdBy` | String | User ID who created the assignment (Keycloak UUID) |
| `status` | DoctorAssignmentStatus | ACTIVE or INACTIVE |
| `assignedAt` | LocalDateTime | When assignment was created |
| `notes` | String | Additional notes about the assignment |

### ChecklistItem Entity

Daily task items created by doctors and completed by caregivers.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key (auto-generated) |
| `doctorId` | String | Reference to doctor who created the item (Keycloak UUID) |
| `patientId` | String | Reference to patient user (Keycloak UUID) |
| `date` | LocalDate | Date for which the item is scheduled |
| `itemOrder` | Integer | Display order for the item |
| `description` | String | Task description |
| `priority` | ChecklistPriority | LOW, MEDIUM, or HIGH |
| `status` | ChecklistStatus | PENDING, ASSIGNED, or COMPLETED |
| `category` | ChecklistCategory | MEDICATION, INCIDENT, COGNITIVE_TEST, or GENERAL |
| `assignedCaregiverId` | String | Reference to assigned caregiver (Keycloak UUID) |
| `completedBy` | String | Reference to user who completed the task (Keycloak UUID) |
| `completedAt` | LocalDateTime | Completion timestamp |
| `sourceAxisReference` | String | Reference to source axis/event that generated this item |
| `checklistGeneratedAt` | LocalDateTime | When the checklist was generated |
| `checklistStatus` | ChecklistCompletionStatus | PENDING or COMPLETED |

**Note:** Checklist items are automatically generated or manually created by doctors for daily care coordination.

### CaregiverHandover Entity

Facilitates transfer of care information between caregivers.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key (auto-generated) |
| `patientId` | String | Reference to patient user (Keycloak UUID) |
| `fromCaregiverId` | String | Caregiver transferring the information (Keycloak UUID) |
| `toCaregiverId` | String | Caregiver receiving the information (Keycloak UUID) |
| `notes` | String | Handover notes and important information |
| `acknowledged` | Boolean | Whether handover has been acknowledged |
| `createdAt` | LocalDateTime | When handover was created |
| `acknowledgedAt` | LocalDateTime | When handover was acknowledged |

---

## Enums

### CaregiverRole
| Value | Description |
|-------|-------------|
| `PRIMARY` | Main caregiver with full access |
| `FAMILY` | Family member with limited access |
| `EMERGENCY` | Emergency contact only |

### AssignmentStatus
| Value | Description |
|-------|-------------|
| `PENDING` | Invitation sent, awaiting acceptance |
| `ACTIVE` | Assignment is active and valid |
| `REVOKED` | Access has been revoked |

### DoctorAssignmentStatus
| Value | Description |
|-------|-------------|
| `ACTIVE` | Doctor is actively treating patient |
| `INACTIVE` | Assignment has been deactivated |

### ChecklistPriority
| Value | Description |
|-------|-------------|
| `LOW` | Low priority task |
| `MEDIUM` | Medium priority task |
| `HIGH` | High priority task requiring immediate attention |

### ChecklistStatus
| Value | Description |
|-------|-------------|
| `PENDING` | Item created but not yet assigned |
| `ASSIGNED` | Assigned to a caregiver |
| `COMPLETED` | Task has been completed |

### ChecklistCategory
| Value | Description |
|-------|-------------|
| `MEDICATION` | Medication-related task |
| `INCIDENT` | Incident follow-up task |
| `COGNITIVE_TEST` | Cognitive assessment task |
| `GENERAL` | General care task |

### ChecklistCompletionStatus
| Value | Description |
|-------|-------------|
| `PENDING` | Checklist not yet completed |
| `COMPLETED` | All items in checklist completed |

---

## Caregiver Invite Flow

The secure invitation system for onboarding caregivers:

```
┌─────────────┐     Generate Invite      ┌─────────────────┐
│   Doctor/   │ ───────────────────────► │  Caregiver      │
│   Admin     │                          │  Assignment     │
└─────────────┘                          │  (PENDING)      │
                                         └────────┬────────┘
                                                  │
                                                  │ Token sent
                                                  │ via secure channel
                                                  ▼
                                         ┌─────────────────┐
                                         │   Caregiver     │
                                         │   Validates     │
                                         │   Token         │
                                         └────────┬────────┘
                                                  │
                                                  │ Accept Invite
                                                  ▼
                                         ┌─────────────────┐
                                         │  Caregiver      │
                                         │  Assignment     │
                                         │  (ACTIVE)       │
                                         └─────────────────┘
```

**Steps:**
1. Doctor/Admin calls `POST /caregivers/generate-invite` with patientId and caregiver info
2. System creates a PENDING assignment with secure invite token
3. Token is shared with caregiver via secure channel (email/SMS)
4. Caregiver validates token via `GET /invitations/{token}/validate`
5. Caregiver accepts invite via `POST /invitations/{token}/accept`
6. Assignment status changes to ACTIVE

---

## Frontend Integration Guide

### Base URL

All API calls should go through the Gateway:

```
http://localhost:8080/api/v1/care-team
```

### Authentication

Include the JWT token in the Authorization header:

```
Authorization: Bearer <access_token>
```

The Gateway validates the JWT against Keycloak before forwarding to the Care Team Service.

### Example Requests

#### Generate Caregiver Invite
```http
POST http://localhost:8080/api/v1/care-team/caregivers/generate-invite
Authorization: Bearer <token>
Content-Type: application/json

{
  "patientId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "caregiverId": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
  "role": "FAMILY"
}
```

#### Accept Caregiver Invite
```http
POST http://localhost:8080/api/v1/care-team/invitations/abc123xyz/accept
Authorization: Bearer <token>
```

#### List Patient's Caregivers
```http
GET http://localhost:8080/api/v1/care-team/patients/6ba7b810-9dad-11d1-80b4-00c04fd430c8/caregivers
Authorization: Bearer <token>
```

#### Assign Doctor to Patient
```http
POST http://localhost:8080/api/v1/care-team/doctors/550e8400-e29b-41d4-a716-446655440000/patients/create
Authorization: Bearer <token>
Content-Type: application/json

{
  "patientId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "notes": "Primary neurologist assignment"
}
```

#### Create Checklist Item
```http
POST http://localhost:8080/api/v1/care-team/checklists/items
Authorization: Bearer <token>
Content-Type: application/json

{
  "doctorId": "550e8400-e29b-41d4-a716-446655440000",
  "patientId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "date": "2026-02-28",
  "itemOrder": 1,
  "description": "Administer morning medication",
  "priority": "HIGH",
  "category": "MEDICATION"
}
```

#### Assign Checklist Item to Caregiver
```http
PUT http://localhost:8080/api/v1/care-team/checklists/items/550e8400-e29b-41d4-a716-446655440001/assign
Authorization: Bearer <token>
Content-Type: application/json

{
  "caregiverId": "6ba7b811-9dad-11d1-80b4-00c04fd430c8"
}
```

#### Complete Checklist Item
```http
PUT http://localhost:8080/api/v1/care-team/checklists/items/550e8400-e29b-41d4-a716-446655440001/complete
Authorization: Bearer <token>
Content-Type: application/json

{
  "completedBy": "6ba7b811-9dad-11d1-80b4-00c04fd430c8"
}
```

#### Create Handover Note
```http
POST http://localhost:8080/api/v1/care-team/caregivers/handover
Authorization: Bearer <token>
Content-Type: application/json

{
  "patientId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "fromCaregiverId": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
  "toCaregiverId": "6ba7b812-9dad-11d1-80b4-00c04fd430c8",
  "notes": "Patient was restless last night. Monitor sleep patterns."
}
```

#### Acknowledge Handover
```http
PUT http://localhost:8080/api/v1/care-team/caregivers/handover/550e8400-e29b-41d4-a716-446655440002/acknowledge
Authorization: Bearer <token>
```

### Authorization Matrix

| Endpoint | PATIENT | CAREGIVER | DOCTOR | ADMIN |
|----------|---------|-----------|--------|-------|
| GET /patients/{id}/caregivers | Own only | Assigned patients | Assigned patients | All |
| POST /caregivers/generate-invite | ❌ | ❌ | ✅ | ✅ |
| GET /doctors/{id}/patients | ❌ | ❌ | Own only | All |
| POST /doctors/{id}/patients/create | ❌ | ❌ | Own only | ✅ |
| POST /checklists/items | ❌ | ❌ | Own only | ✅ |
| PUT /checklists/items/{id}/complete | ❌ | Assigned only | ❌ | ✅ |
| POST /caregivers/handover | ❌ | ✅ | ❌ | ✅ |

---

## Build

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Package
mvn clean package -DskipTests

# Build Docker image
docker build -t alzcare/care-team:latest .
```

## Run

### Local Development
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker
```bash
docker run -p 8008:8008 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/careteam_db \
  -e EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://localhost:8761/eureka \
  alzcare/care-team:latest
```

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Application port | 8008 |
| `SPRING_PROFILES_ACTIVE` | Active profile | dev |
| `SPRING_DATASOURCE_URL` | MySQL connection URL | jdbc:mysql://localhost:3307/careteam_db |
| `SPRING_DATASOURCE_USERNAME` | Database username | root |
| `SPRING_DATASOURCE_PASSWORD` | Database password | root |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | Eureka server URL | http://localhost:8761/eureka |
| `SPRING_CLOUD_CONFIG_URI` | Config server URL | http://localhost:8888 |

---

## Architecture

The Care Team Service follows a layered architecture:

1. **REST API Layer** - Exposes endpoints for care team management
2. **Service Layer** - Business logic for assignments and checklists
3. **Repository Layer** - JPA repositories for MySQL entities
4. **Integration Layer** - Eureka client for service discovery

### Service Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                     CARE TEAM SERVICE                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────┐    ┌──────────────┐    ┌──────────────────────┐   │
│  │   Client    │───►│   Gateway    │───►│  REST Controllers    │   │
│  └─────────────┘    └──────────────┘    └──────────┬───────────┘   │
│                                                    │                │
│         ┌──────────────────────────────────────────┼────────┐       │
│         │                                          │        │       │
│         ▼                                          ▼        ▼       │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────────┐      │
│  │   Caregiver    │  │    Doctor      │  │    Checklist     │      │
│  │  Assignment    │  │  Assignment    │  │     Service      │      │
│  │    Service     │  │    Service     │  │                  │      │
│  └───────┬────────┘  └───────┬────────┘  └─────────┬────────┘      │
│          │                   │                     │                │
│          ▼                   ▼                     ▼                │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                         MySQL Database                       │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │   │
│  │  │ Caregiver   │  │   Doctor    │  │    ChecklistItem    │  │   │
│  │  │ Assignments │  │ Assignments │  │                     │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘  │   │
│  │  ┌─────────────────────────────────────────────────────────┐ │   │
│  │  │              CaregiverHandover                          │ │   │
│  │  └─────────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              Eureka Client (Service Discovery)               │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Service Communication

```
┌─────────────────────────────────────────────────────────────────┐
│                         ALZCARE PLATFORM                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────┐    ┌──────────────┐    ┌──────────────────┐   │
│  │   Client    │───►│   Gateway    │───►│  Care Team       │   │
│  │   (Angular) │    │   (8080)     │    │  Service (8008)  │   │
│  └─────────────┘    └──────────────┘    └────────┬─────────┘   │
│                                                   │              │
│                     ┌─────────────────────────────┘              │
│                     │                                           │
│                     ▼                                           │
│            ┌──────────────────┐    ┌──────────────────┐        │
│            │   Identity       │◄──►│   Notification   │        │
│            │   Service (8001) │    │   Service        │        │
│            └──────────────────┘    └──────────────────┘        │
│                                                   │              │
│                                                   ▼              │
│                                          ┌──────────────────┐   │
│                                          │   Safety Alert   │   │
│                                          │   Engine         │   │
│                                          └──────────────────┘   │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │   Keycloak   │  │   Eureka     │  │   Config Server      │  │
│  │   (8090)     │  │   (8761)     │  │   (8888)             │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Dependencies

- Spring Boot 3.2.0
- Spring Data JPA
- Spring Security
- Spring Cloud Netflix Eureka Client
- Spring Cloud Config Client
- MySQL Connector/J
- Lombok for boilerplate reduction
- Swagger/OpenAPI for API documentation

---

## Testing

Run tests with:
```bash
mvn test
```

---

## License

Copyright © 2025 AlzCare Platform
