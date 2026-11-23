# AlzCare Platform - Entities & Enums for Class Diagram

## Entities

### 1. User (Keycloak / External Identity Provider)
| Attribute | Type | Notes |
|-----------|------|-------|
| id | UUID | Keycloak UUID (PK) |
| username | String | Login identifier |
| email | String | |
| status | UserStatus | ACTIVE, INACTIVE, PENDING |
| role | UserRole | PATIENT, DOCTOR, CAREGIVER, ADMIN |
| enabled | Boolean | Account enabled/disabled |
| emailVerified | Boolean | |
| createdAt | Instant | |
| lastLogin | Instant | |

**Note:** User lives in Keycloak (auth only). Names are stored in respective profiles.

---

### 2. PatientProfile (Aggregate Root)
| Attribute | Type | Notes |
|-----------|------|-------|
| id | UUID | Internal UUID (PK) |
| userId | UUID | Link to Keycloak user |
| firstName | String | |
| lastName | String | |
| dateOfBirth | LocalDate | |
| gender | GenderEnum | MALE, FEMALE |
| preferredLanguage | LanguageEnum | ARABIC, FRENCH, ENGLISH |
| photoUrl | String | |
| phone | String | |
| emergencyContact | String | |
| address | String | |
| isActive | Boolean | Profile active status |
| autonomyAssessments | List<AutonomyAssessment> | 1-to-many relationship |
| version | Long | Optimistic locking |
| updatedAt | Instant | |
| createdAt | Instant | |
| createdBy | UUID | Who created the profile (Doctor/Admin ID) |
| totalPoints | Integer | Game points (temporary, may move to Game MS) |
| currentStreak | Integer | Game streak (temporary, may move to Game MS) |
| lastPlayedDate | LocalDate | Last game session (temporary, may move to Game MS) |
| assistedModeActive | Boolean | Game setting (temporary, may move to Game MS) |

---

### 3. DoctorProfile
| Attribute | Type | Notes |
|-----------|------|-------|
| id | UUID | PK |
| userId | UUID | Link to Keycloak user |
| firstName | String | |
| lastName | String | |
| photoUrl | String | |
| speciality | String | Medical specialty |
| licenseNumber | String | Medical license |
| phone | String | |
| contact | String | Flexible contact (email, social, etc.) |
| address | String | Practice address |
| isAvailable | Boolean | Currently accepting patients |
| updatedAt | Instant | |
| createdAt | Instant | |

---

### 4. CaregiverProfile
| Attribute | Type | Notes |
|-----------|------|-------|
| id | UUID | PK |
| userId | UUID | Link to Keycloak user |
| firstName | String | |
| lastName | String | |
| photoUrl | String | |
| phone | String | |
| address | String | |
| contact | String | Flexible contact (email, social, etc.) |
| isAvailable | Boolean | Available for assignments |
| isProfessional | Boolean | Paid professional vs family caregiver |
| updatedAt | Instant | |
| createdAt | Instant | |

---

### 5. AutonomyAssessment
| Attribute | Type | Notes |
|-----------|------|-------|
| id | UUID | PK |
| patientId | UUID | Reference to PatientProfile |
| assessedBy | UUID | Doctor who performed assessment (Keycloak ID) |
| assessmentDate | LocalDate | When assessment was done |
| hygieneLevel | AutonomyLevel | INDEPENDENT, ASSISTED, DEPENDENT |
| medicationLevel | AutonomyLevel | INDEPENDENT, ASSISTED, DEPENDENT |
| mobilityLevel | AutonomyLevel | INDEPENDENT, ASSISTED, DEPENDENT |
| feedingLevel | AutonomyLevel | INDEPENDENT, ASSISTED, DEPENDENT |
| overallScore | Integer | 1-10 calculated score |
| notes | String | Additional observations |

---

## Enums

### UserRole
```
PATIENT
DOCTOR
CAREGIVER
ADMIN
```

### UserStatus
```
ACTIVE
INACTIVE
PENDING
```

### GenderEnum
```
MALE
FEMALE
```

### LanguageEnum
```
ARABIC
FRENCH
ENGLISH
```

### AutonomyLevel
```
INDEPENDENT
ASSISTED
DEPENDENT
```

---

## Relationships (for Class Diagram)

### Visual Hierarchy (Role Classes)
```
                              ┌─────────────────────┐
                              │        User         │
                              │    (Keycloak)       │
                              │       (1)           │
                              └──────────┬──────────┘
                                         │
            ┌──────────────┬─────────────┼─────────────┬──────────────┐
            │              │             │             │              │
            ▼              ▼             ▼             ▼              ▼
      ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
      │ Patient  │  │  Doctor  │  │  Admin   │  │ Caregiver│  │(Abstract)│
      │  (Role)  │  │  (Role)  │  │  (Role)  │  │  (Role)  │  │          │
      └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────────┘
           │             │             │             │
           ▼             ▼             │             ▼
    ┌──────────────┐ ┌──────────────┐  │      ┌──────────────┐
    │PatientProfile│ │DoctorProfile │  │      │CaregiverProfile
    └──────┬───────┘ └──────────────┘  │      └──────────────┘
           │                           │
           │ 1..*                      │
           ▼                           │
    ┌──────────────┐                   │
    │AutonomyAsmt  │                   │
    └──────────────┘                   │
                                       │
                              (No profile for Admin)
```

**Notes:**
- **User** is stored in Keycloak (external), linked to profiles via `userId` (which is Keycloak ID)
- **Patient, Doctor, Admin, Caregiver** are visual role classes (no attributes) to show inheritance
- **PatientProfile** has a 1-to-many relationship with **AutonomyAssessment**
- **Admin** has no profile - uses Keycloak user directly
- Game-related fields in PatientProfile (`totalPoints`, `currentStreak`, `lastPlayedDate`, `assistedModeActive`) are temporary and may move to a separate Game/Cognitive Microservice later
- `createdBy` in PatientProfile tracks which Doctor or Admin created the patient account

---

## Database Tables (Identity MS PostgreSQL)

| Table | Purpose |
|-------|---------|
| `patient_profiles` | Patient demographics, preferences, game data |
| `doctor_profiles` | Doctor professional details |
| `caregiver_profiles` | Caregiver relationship/experience data |
| `autonomy_assessments` | Patient autonomy assessment history |

---

## API Endpoints (for context)

| Profile | Endpoint |
|---------|----------|
| Patient | `GET /api/v1/patients/{keycloakId}` |
| Doctor | `GET /api/v1/doctors/user/{userId}` |
| Caregiver | `GET /api/v1/caregivers/user/{userId}` |
