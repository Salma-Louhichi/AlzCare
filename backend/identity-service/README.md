# Identity Service

Patient profile management and user administration service for AlzCare Platform. Acts as a bridge between the application and Keycloak for identity management.

## Port
- 8001

## Database
- PostgreSQL (port 5432)

## Features
- **User Management** - Create, update, delete users in Keycloak
- **Profile Management** - Patient, Doctor, and Caregiver profile CRUD
- **Autonomy Assessment** - Track patient autonomy levels across 4 domains
- **Admin APIs** - Full user and profile administration
- **Profile Auto-Creation** - Automatic profile creation based on user role
- **Risk Factor Tracking** *(Planned)* - Wandering, falls, medication adherence risks
- **Care Preferences** *(Planned)* - Language, cultural context, notification preferences

---

## API Endpoints

### Patient Profile Endpoints
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/v1/patients` | Create new patient profile | ✅ Implemented |
| GET | `/api/v1/patients` | Get all patient profiles (optional `isActive` filter) | ✅ Implemented |
| GET | `/api/v1/patients/{userId}` | Get patient profile by Keycloak user ID | ✅ Implemented |
| PUT | `/api/v1/patients/{id}` | Update patient profile by internal UUID | ✅ Implemented |

### Doctor Profile Endpoints
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/v1/doctors/{id}` | Get doctor profile by internal UUID | ✅ Implemented |
| GET | `/api/v1/doctors/user/{userId}` | Get doctor profile by Keycloak user ID | ✅ Implemented |
| POST | `/api/v1/doctors` | Create doctor profile | 📝 Planned |
| PUT | `/api/v1/doctors/{id}` | Update doctor profile | 📝 Planned |

### Caregiver Profile Endpoints
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/v1/caregivers/{id}` | Get caregiver profile by internal UUID | ✅ Implemented |
| GET | `/api/v1/caregivers/user/{userId}` | Get caregiver profile by Keycloak user ID | ✅ Implemented |
| POST | `/api/v1/caregivers` | Create caregiver profile | 📝 Planned |
| PUT | `/api/v1/caregivers/{id}` | Update caregiver profile | 📝 Planned |

### Autonomy Assessment Endpoints
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/v1/patients/{patientId}/autonomy` | Get patient's latest autonomy assessment | ✅ Implemented |
| POST | `/api/v1/patients/{patientId}/autonomy` | Submit new autonomy assessment | ✅ Implemented |
| GET | `/api/v1/patients/{patientId}/autonomy/history` | Get autonomy assessment history | ✅ Implemented |

### Risk Factor Endpoints *(Planned)*
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/v1/patients/{id}/risk-factors` | List patient's risk factors | 📝 Planned |
| POST | `/api/v1/patients/{id}/risk-factors` | Add new risk factor | 📝 Planned |
| PUT | `/api/v1/patients/{id}/risk-factors/{riskId}` | Update risk factor | 📝 Planned |
| DELETE | `/api/v1/patients/{id}/risk-factors/{riskId}` | Remove risk factor | 📝 Planned |

### Admin User Management Endpoints
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/v1/admin/users` | List all users (with search, role, status filters, pagination) | ✅ Implemented |
| GET | `/api/v1/admin/users/{userId}` | Get user details by Keycloak ID | ✅ Implemented |
| POST | `/api/v1/admin/users` | Create new user (auto-creates profile based on role) | ✅ Implemented |
| PUT | `/api/v1/admin/users/{userId}` | Update user | ✅ Implemented |
| DELETE | `/api/v1/admin/users/{userId}` | Delete user | ✅ Implemented |
| POST | `/api/v1/admin/users/{userId}/reset-password` | Reset user password | ✅ Implemented |

### Admin Profile Management Endpoints
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/v1/admin/profiles/{userId}` | Get any user's profile by role | ✅ Implemented |
| GET | `/api/v1/admin/profiles/{userId}/exists` | Check if profile exists | ✅ Implemented |
| PUT | `/api/v1/admin/profiles/{userId}` | Update profile (generic - uses role-specific request body) | ✅ Implemented |
| PUT | `/api/v1/admin/profiles/{userId}/patient` | Update patient profile | ✅ Implemented |
| PUT | `/api/v1/admin/profiles/{userId}/doctor` | Update doctor profile | ✅ Implemented |
| PUT | `/api/v1/admin/profiles/{userId}/caregiver` | Update caregiver profile | ✅ Implemented |

### Actuator
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check endpoint |
| GET | `/actuator/info` | Service information |

---

## Entity Documentation

### PatientProfile Entity

Core patient profile data linked to Keycloak user.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key (internal) |
| `userId` | UUID | Keycloak user UUID |
| `firstName` | String | Patient first name (max 100 chars) |
| `lastName` | String | Patient last name (max 100 chars) |
| `dateOfBirth` | LocalDate | Birth date |
| `gender` | GenderEnum | MALE or FEMALE |
| `preferredLanguage` | LanguageEnum | ARABIC, FRENCH, or ENGLISH (default: ENGLISH) |
| `photoUrl` | String | Cloudinary URL for patient photo (max 500 chars) |
| `phone` | String | Contact phone number (max 50 chars) |
| `emergencyContact` | String | Emergency contact info (max 255 chars) |
| `address` | String | Physical address (TEXT) |
| `isActive` | Boolean | Profile active status (default: true) |
| `totalPoints` | Integer | Gamification total points |
| `currentStreak` | Integer | Gamification current streak |
| `lastPlayedDate` | LocalDate | Last game activity date |
| `assistedModeActive` | Boolean | Whether assisted mode is active |
| `version` | Long | JPA optimistic locking version |
| `createdBy` | UUID | User ID who created the profile |
| `createdAt` | OffsetDateTime | Creation timestamp |
| `updatedAt` | OffsetDateTime | Last update timestamp |

### DoctorProfile Entity

Doctor-specific profile data.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key (internal) |
| `userId` | UUID | Keycloak user UUID |
| `firstName` | String | Doctor first name (max 100 chars) |
| `lastName` | String | Doctor last name (max 100 chars) |
| `photoUrl` | String | Cloudinary URL for photo (max 500 chars) |
| `speciality` | String | Medical specialty (max 200 chars) |
| `licenseNumber` | String | Medical license number (max 100 chars, unique) |
| `phone` | String | Contact phone (max 50 chars) |
| `contact` | String | Additional contact info (max 255 chars) |
| `address` | String | Physical address (max 500 chars) |
| `isAvailable` | Boolean | Availability status (default: true) |
| `createdAt` | OffsetDateTime | Creation timestamp |
| `updatedAt` | OffsetDateTime | Last update timestamp |

### CaregiverProfile Entity

Caregiver-specific profile data.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key (internal) |
| `userId` | UUID | Keycloak user UUID |
| `firstName` | String | Caregiver first name (max 100 chars) |
| `lastName` | String | Caregiver last name (max 100 chars) |
| `photoUrl` | String | Cloudinary URL for photo (max 500 chars) |
| `phone` | String | Contact phone (max 50 chars) |
| `address` | String | Physical address (max 255 chars) |
| `contact` | String | Additional contact info (max 255 chars) |
| `isAvailable` | Boolean | Availability status (default: true) |
| `isProfessional` | Boolean | Whether professional caregiver (default: false) |
| `createdAt` | OffsetDateTime | Creation timestamp |
| `updatedAt` | OffsetDateTime | Last update timestamp |

### AutonomyAssessment Entity

Patient autonomy level assessment across 4 domains.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `patient` | PatientProfile | Reference to patient (Many-to-One) |
| `hygieneLevel` | AutonomyLevel | INDEPENDENT, ASSISTED, or DEPENDENT |
| `medicationLevel` | AutonomyLevel | INDEPENDENT, ASSISTED, or DEPENDENT |
| `mobilityLevel` | AutonomyLevel | INDEPENDENT, ASSISTED, or DEPENDENT |
| `feedingLevel` | AutonomyLevel | INDEPENDENT, ASSISTED, or DEPENDENT |
| `overallScore` | Integer | Calculated score (1-10 scale) |
| `assessedBy` | UUID | User ID of assessor |
| `notes` | String | Additional notes (TEXT) |
| `assessmentDate` | LocalDate | Date of assessment |
| `createdAt` | OffsetDateTime | Creation timestamp |

**Note:** The `overallScore` is automatically calculated from the 4 domain levels:
- INDEPENDENT = 3 points
- ASSISTED = 2 points  
- DEPENDENT = 1 point
- Sum (4-12) is mapped to 1-10 scale: `(sum - 4) * 10 / 8 + 1`

### RiskFactor Entity *(Planned)*

Risk factors for patients (wandering, falls, etc.). Not yet implemented.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `patientId` | UUID | Reference to PatientProfile |
| `type` | RiskType | WANDERING, FALLS, MEDICATION_ADHERENCE, etc. |
| `severity` | RiskSeverity | LOW, MEDIUM, HIGH |
| `description` | String | Detailed description |
| `triggers` | String | Known triggers for the risk |
| `mitigation` | String | Mitigation strategies |
| `isActive` | Boolean | Whether risk is currently active |
| `identifiedAt` | LocalDate | When risk was identified |
| `reviewDate` | LocalDate | Next review date |
| `createdAt` | Timestamp | Creation timestamp |
| `updatedAt` | Timestamp | Last update timestamp |

### CarePreferences Entity *(Planned)*

Patient preferences for care and notifications. Not yet implemented.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `patientId` | UUID | Reference to PatientProfile |
| `preferredLanguage` | LanguageEnum | ARABIC, FRENCH, ENGLISH |
| `notificationChannels` | List | Preferred channels: SMS, EMAIL, PUSH, IN_APP |
| `quietHoursStart` | Time | Start of quiet hours (no notifications) |
| `quietHoursEnd` | Time | End of quiet hours |
| `timezone` | String | Patient's timezone |
| `createdAt` | Timestamp | Creation timestamp |
| `updatedAt` | Timestamp | Last update timestamp |

---

## Enums

### GenderEnum
| Value | Description |
|-------|-------------|
| `MALE` | Male gender |
| `FEMALE` | Female gender |

### LanguageEnum
| Value | Description |
|-------|-------------|
| `ARABIC` | Arabic language |
| `FRENCH` | French language |
| `ENGLISH` | English language (default) |

### AutonomyLevel
| Value | Points | Description |
|-------|--------|-------------|
| `INDEPENDENT` | 3 | Fully independent |
| `ASSISTED` | 2 | Requires assistance |
| `DEPENDENT` | 1 | Fully dependent |

### RiskType *(Planned)*
| Value | Description |
|-------|-------------|
| `WANDERING` | Wandering risk |
| `FALLS` | Fall risk |
| `MEDICATION_ADHERENCE` | Medication adherence risk |
| `AGGRESSION` | Aggression risk |
| `CONFUSION` | Confusion/disorientation risk |

### RiskSeverity *(Planned)*
| Value | Description |
|-------|-------------|
| `LOW` | Low risk level |
| `MEDIUM` | Medium risk level |
| `HIGH` | High risk level |

---

## Profile Auto-Creation

When an admin creates a user via `POST /api/v1/admin/users`, the system automatically creates:

| Role | Profile Created |
|------|-----------------|
| `PATIENT` | PatientProfile |
| `DOCTOR` | DoctorProfile |
| `CAREGIVER` | CaregiverProfile |
| `ADMIN` | No profile (Keycloak only) |

**Flow:**
1. Admin calls `POST /api/v1/admin/users` with user details
2. KeycloakAdminService creates user in Keycloak via Admin API
3. Role is assigned (PATIENT, DOCTOR, CAREGIVER, or ADMIN)
4. Profile is created in local PostgreSQL based on role

---

## RabbitMQ Events *(Planned)*

The Identity Service will publish events to RabbitMQ for other services to consume.

**Exchange:** `profile.updates` *(Planned)*

| Event Type | Description | Payload |
|------------|-------------|---------|
| `ProfileUpdated` | Patient data changed | patientId, changedFields, timestamp |
| `AutonomyChanged` | New assessment submitted | patientId, newScore, assessedBy, timestamp |

---

## Keycloak Integration

### Dual-Database Architecture

| Aspect | Storage |
|--------|---------|
| **Authentication** | Keycloak (users, passwords, roles, tokens) |
| **Profile Data** | PostgreSQL (extended profile data) |

### Keycloak Realm Roles

| Role | Description |
|------|-------------|
| `PATIENT` | Patient user with access to own profile |
| `CAREGIVER` | Caregiver with access to assigned patients |
| `DOCTOR` | Doctor with medical access |
| `ADMIN` | System administrator with full platform access |

### Keycloak Clients

| Client | Purpose |
|--------|---------|
| `alzcare-webapp` | Angular web app for patients/caregivers (public client) |
| `alzcare-admin` | Angular admin dashboard (public client) |
| `alzcare-gateway` | API Gateway token validation (service account) |

---

## Frontend Integration Guide

### Base URL

All API calls should go through the Gateway:

```
http://localhost:8080/identity
```

### Authentication

Include the JWT token in the Authorization header:

```
Authorization: Bearer <access_token>
```

The Gateway validates the JWT against Keycloak before forwarding to the Identity Service.

### Example Requests

#### Create Patient Profile
```http
POST http://localhost:8080/identity/api/v1/patients
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": "keycloak-uuid",
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1950-05-15",
  "gender": "MALE",
  "preferredLanguage": "ENGLISH",
  "phone": "+1234567890",
  "address": "123 Main St",
  "emergencyContact": "Jane Doe - Spouse - +0987654321"
}
```

#### Get Patient Profile by Keycloak ID
```http
GET http://localhost:8080/identity/api/v1/patients/{userId}
Authorization: Bearer <token>
```

#### Submit Autonomy Assessment
```http
POST http://localhost:8080/identity/api/v1/patients/{patientId}/autonomy
Authorization: Bearer <token>
Content-Type: application/json

{
  "hygieneLevel": "ASSISTED",
  "medicationLevel": "DEPENDENT",
  "mobilityLevel": "INDEPENDENT",
  "feedingLevel": "ASSISTED",
  "assessedBy": "doctor-uuid",
  "assessmentDate": "2026-02-21",
  "notes": "Patient needs medication assistance"
}
```

#### Admin: Create User with Auto-Profile
```http
POST http://localhost:8080/identity/api/v1/admin/users
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "username": "newpatient",
  "email": "patient@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "PATIENT",
  "temporaryPassword": "TempPass123!",
  "enabled": true
}
```

#### Admin: Update Patient Profile
```http
PUT http://localhost:8080/identity/api/v1/admin/profiles/{userId}/patient
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "firstName": "Johnny",
  "phone": "+1111111111",
  "address": "Updated Address"
}
```

### Authorization Matrix

| Endpoint | PATIENT | CAREGIVER | DOCTOR | ADMIN |
|----------|---------|-----------|--------|-------|
| GET /patients/{userId} | Own only | ❌ | ❌ | All |
| PUT /patients/{id} | Own profile | ❌ | ❌ | All via Admin APIs |
| POST /patients/{id}/autonomy | ❌ | ❌ | ✅ | ✅ |
| GET /doctors/** | ❌ | ❌ | ❌ | ✅ |
| GET /caregivers/** | ❌ | ❌ | ❌ | ✅ |
| /admin/users/** | ❌ | ❌ | ❌ | ✅ |
| /admin/profiles/** | ❌ | ❌ | ❌ | ✅ |

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
docker build -t alzcare/identity:latest .
```

## Run

### Local Development
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker
```bash
docker run -p 8001:8001 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/identity_db \
  -e KEYCLOAK_SERVER_URL=http://localhost:8090 \
  alzcare/identity:latest
```

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Application port | 8001 |
| `SPRING_PROFILES_ACTIVE` | Active profile | dev |
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | jdbc:postgresql://localhost:5432/identity_db |
| `SPRING_DATASOURCE_USERNAME` | Database username | postgres |
| `SPRING_DATASOURCE_PASSWORD` | Database password | postgres |
| `KEYCLOAK_SERVER_URL` | Keycloak server URL | http://localhost:8090 |
| `KEYCLOAK_REALM` | Keycloak realm | master |
| `KEYCLOAK_ADMIN_USERNAME` | Keycloak admin username | admin |
| `KEYCLOAK_ADMIN_PASSWORD` | Keycloak admin password | admin |
| `SPRING_RABBITMQ_HOST` | RabbitMQ host | localhost |
| `SPRING_RABBITMQ_PORT` | RabbitMQ port | 5672 |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | Eureka server URL | http://localhost:8761/eureka |

---

## Architecture

The Identity Service follows a layered architecture:

1. **REST API Layer** - Exposes endpoints for profile management
2. **Keycloak Admin Client** - Manages users, roles, and authentication in Keycloak
3. **Profile Service Layer** - Handles CRUD for patient/doctor/caregiver profiles
4. **Repository Layer** - JPA repositories for PostgreSQL entities

### Service Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     IDENTITY SERVICE                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────┐    ┌──────────────┐    ┌──────────────────┐   │
│  │   Client    │───►│   Gateway    │───►│  REST Controller │   │
│  └─────────────┘    └──────────────┘    └────────┬─────────┘   │
│                                                   │              │
│                    ┌──────────────────────────────┼────────┐    │
│                    │                              │        │    │
│                    ▼                              ▼        ▼    │
│           ┌──────────────┐              ┌──────────────────┐   │
│           │   Profile    │              │  Keycloak Admin  │   │
│           │   Services   │              │     Service      │   │
│           └──────┬───────┘              └────────┬─────────┘   │
│                  │                               │              │
│                  ▼                               ▼              │
│           ┌──────────────┐              ┌──────────────────┐   │
│           │  PostgreSQL  │              │     Keycloak     │   │
│           │  (Profiles)  │              │   (Auth/Users)   │   │
│           └──────────────┘              └──────────────────┘   │
│                  │                                              │
│                  └──────────────────────► RabbitMQ (Planned)    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Dependencies

- Spring Boot 3.2.2
- Spring Data JPA
- Spring Cloud Netflix Eureka Client
- Spring Cloud Config Client
- Keycloak Admin Client 22.0.5
- RESTEasy Client 6.2.7.Final
- PostgreSQL Driver
- MapStruct for mapping
- Lombok for boilerplate reduction
- Swagger/OpenAPI for API documentation

---

## Testing

Integration tests use Testcontainers for:
- PostgreSQL

Run tests with:
```bash
mvn test
```

---

## License

Copyright © 2025 AlzCare Platform
