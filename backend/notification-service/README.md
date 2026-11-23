# Notification Service

Multi-channel notification delivery service for the AlzCare Platform. This microservice handles all notification needs across the platform—from safety alerts triggered by the Safety Alert Engine to scheduled medication reminders configured via the admin dashboard.

## Port
- **8004**

## Database
- **MongoDB** (port 27017)

---

## 🔔 What This Service Does

The Notification Service is the **central communication hub** of the AlzCare platform. It receives notification requests from multiple sources, determines the best delivery channel, and ensures messages reach users reliably.

### Key Capabilities

| Capability | Description |
|------------|-------------|
| **Real-time Alerts** | Consumes safety alerts from RabbitMQ (falls, wandering) and delivers them instantly via SMS/Email/Push |
| **Dynamic Scheduling** | Admins can create scheduled notification campaigns without code changes—hydration reminders, check-ins, medication alerts |
| **Multi-Channel Delivery** | Routes notifications through SMS, Email, Push, or In-App channels based on priority and user preferences |
| **Template Management** | Reusable message templates with variable substitution (`{{firstName}}`, `{{currentDate}}`) |
| **Retry Mechanism** | Automatically retries failed deliveries up to 3 times with exponential backoff |
| **User Targeting** | Targets users by role (PATIENT, CAREGIVER, DOCTOR) or specific user IDs via OpenFeign integration |

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        NOTIFICATION SERVICE ARCHITECTURE                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────────┐  │
│  │   REST API       │    │   RabbitMQ       │    │   Dynamic Scheduler  │  │
│  │   (Manual Send)  │    │   (Alert Events) │    │   (Scheduled Rules)  │  │
│  └────────┬─────────┘    └────────┬─────────┘    └──────────┬───────────┘  │
│           │                       │                         │              │
│           └───────────────────────┼─────────────────────────┘              │
│                                   ▼                                        │
│                      ┌────────────────────────┐                           │
│                      │  NotificationProcessor │                           │
│                      │  (Priority & Routing)  │                           │
│                      └───────────┬────────────┘                           │
│                                  │                                        │
│           ┌──────────────────────┼──────────────────────┐                 │
│           ▼                      ▼                      ▼                 │
│  ┌────────────────┐  ┌──────────────────┐  ┌──────────────────────┐      │
│  │ Channel Router │  │ Retry Scheduler  │  │ MongoDB Persistence  │      │
│  └───────┬────────┘  │ (Every 60s)      │  └──────────────────────┘      │
│          │           └──────────────────┘                                 │
│   ┌──────┴──────┐                                                          │
│   ▼             ▼                                                          │
│ ┌──────┐  ┌──────────┐  ┌────────┐  ┌────────┐                          │
│ │ SMS  │  │  Email   │  │  Push  │  │ In-App │                          │
│ │Mock* │  │  Mock*   │  │ Mock*  │  │(Mongo) │                          │
│ └──────┘  └──────────┘  └────────┘  └────────┘                          │
│                                                                              │
│ * Ready for Twilio/SendGrid/Firebase integration                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Data Flow Examples

**1. Safety Alert Flow (Real-time)**
```
Patient falls → Safety Alert Engine → RabbitMQ → Notification Service 
→ SMS to Caregiver + Email to Doctor
```

**2. Scheduled Reminder Flow**
```
Admin creates "Hydration Reminder" schedule (every 3 hours)
→ Stored in MongoDB → Dynamic Scheduler triggers
→ In-App notification sent to all PATIENT users
```

**3. Manual Notification Flow**
```
Doctor sends message via Dashboard → POST /api/v1/notifications
→ Routed to specific patient's In-App
```

---

## 🚀 Features

### 1. Dynamic Notification Scheduler ⭐
Admin-configurable scheduled notifications with **no code changes required**.

- **Role-based targeting**: PATIENT, CAREGIVER, DOCTOR, or ALL
- **Flexible scheduling**:
  - **INTERVAL**: Every X minutes (e.g., every 180 minutes = 3 hours)
  - **CRON**: Cron expressions (e.g., `0 0 9 * * *` = daily at 9 AM)
  - **ONE_TIME**: Single execution at specific time
- **Template variables**: `{{userName}}`, `{{firstName}}`, `{{currentDate}}`, `{{currentTime}}`
- **Multi-channel**: IN_APP, SMS, EMAIL, PUSH
- **Timezones**: Cron schedules respect timezone settings
- **Auto-disable**: Rules auto-disable when end date is reached
- **Persistence**: Schedules survive application restarts (loaded from MongoDB on startup)

### 2. Multi-Channel Delivery
| Channel | Status | Integration |
|---------|--------|-------------|
| **IN_APP** | ✅ Active | MongoDB (immediate availability) |
| **SMS** | 🟡 Mock | Ready for Twilio |
| **EMAIL** | 🟡 Mock | Ready for SendGrid |
| **PUSH** | 🟡 Mock | Ready for Firebase |

### 3. Event-Driven Architecture
Consumes events from **safety-alert-engine** via RabbitMQ:
- `AlertCreated` - New safety alert generated
- `NotificationRequested` - Explicit notification request

### 4. Retry Mechanism
- Automatic retry every 60 seconds for failed notifications
- Maximum 3 retry attempts
- Manual retry endpoint available

### 5. Template Management
Reusable notification templates with variable substitution for consistent messaging.

---

## 📡 API Endpoints

### Notifications
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/notifications` | Get all notifications with filters |
| `POST` | `/api/v1/notifications` | Create and send a new notification |
| `GET` | `/api/v1/notifications/{id}` | Get a specific notification by ID |
| `GET` | `/api/v1/notifications/user/{userId}` | Get user notifications with filters |
| `GET` | `/api/v1/notifications/user/{userId}/unread/count` | Get unread notification count |
| `PUT` | `/api/v1/notifications/user/{userId}/mark-all-read` | Mark all as read |
| `PUT` | `/api/v1/notifications/{id}/read` | Mark a notification as read |
| `POST` | `/api/v1/notifications/{id}/retry` | Retry a failed notification |
| `DELETE` | `/api/v1/notifications/{id}` | Delete a notification |

**Query Parameters for `/api/v1/notifications/user/{userId}`:**
- `status` - Filter by status: `PENDING`, `SENT`, `DELIVERED`, `FAILED`, `READ`
- `unread` - Filter by read status: `true` (unread), `false` (read)
- `hours` - Filter to last N hours (e.g., `24` for bell notifications)
- `days` - Filter to last N days (e.g., `7` for notification center)

### Notification Schedules
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/schedules` | List all notification schedules (paginated) |
| `POST` | `/api/v1/schedules` | Create a new notification schedule |
| `GET` | `/api/v1/schedules/{id}` | Get schedule details |
| `PUT` | `/api/v1/schedules/{id}` | Update a schedule |
| `DELETE` | `/api/v1/schedules/{id}` | Delete a schedule |
| `PATCH` | `/api/v1/schedules/{id}/toggle` | Enable/disable a schedule |
| `POST` | `/api/v1/schedules/{id}/trigger` | Manually trigger a schedule |

### Notification Templates
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/notification-templates` | Get all templates |
| `GET` | `/api/v1/notification-templates/{id}` | Get a specific template |
| `POST` | `/api/v1/notification-templates` | Create a new template |
| `PUT` | `/api/v1/notification-templates/{id}` | Update a template |
| `DELETE` | `/api/v1/notification-templates/{id}` | Delete a template |

### Actuator
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/actuator/health` | Health check endpoint |
| `GET` | `/actuator/info` | Service information |
| `GET` | `/actuator/metrics` | Metrics data |

---

## 📊 Entity Documentation

### Notification Entity

MongoDB document representing a notification.

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | MongoDB document ID |
| `recipientId` | String | User ID (UUID) who receives the notification |
| `type` | NotificationType | `ALERT`, `REMINDER`, `SYSTEM`, `EMERGENCY` |
| `channel` | NotificationChannel | `SMS`, `EMAIL`, `PUSH`, `IN_APP` |
| `status` | NotificationStatus | `PENDING`, `SENT`, `DELIVERED`, `READ`, `FAILED` |
| `priority` | NotificationPriority | `LOW`, `NORMAL`, `HIGH`, `CRITICAL` |
| `title` | String | Notification title |
| `message` | String | Notification message content |
| `templateId` | String | Reference to template used (if any) |
| `scheduledAt` | Instant | Scheduled send time (null = immediate) |
| `sentAt` | Instant | Timestamp when notification was sent |
| `deliveredAt` | Instant | Timestamp when notification was delivered |
| `readAt` | Instant | Timestamp when notification was read |
| `retryCount` | Integer | Number of retry attempts (default: 0) |
| `errorMessage` | String | Error message if notification failed |
| `metadata` | Map<String, Object> | Flexible JSON data (alertId, patientId, etc.) |
| `createdAt` | Instant | Document creation timestamp |
| `updatedAt` | Instant | Last update timestamp |

### ScheduledNotificationRule Entity

MongoDB document representing scheduled notification rules.

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Unique schedule rule ID |
| `name` | String | Display name for the rule |
| `description` | String | Admin description |
| `targetRole` | String | `PATIENT`, `CAREGIVER`, `DOCTOR`, `ALL` |
| `targetUserIds` | List<String> | Optional: specific user IDs to target |
| `titleTemplate` | String | Title template with placeholders |
| `messageTemplate` | String | Message body template with placeholders |
| `type` | NotificationType | Notification type |
| `priority` | NotificationPriority | Priority level |
| `channels` | List<NotificationChannel> | Delivery channels |
| `scheduleType` | ScheduleType | `INTERVAL`, `CRON`, `ONE_TIME` |
| `cronExpression` | String | Cron expression (if CRON type) |
| `intervalMinutes` | Integer | Minutes between executions (if INTERVAL) |
| `startDate` | Instant | When the rule becomes active |
| `endDate` | Instant | When the rule expires (optional) |
| `timezone` | String | Timezone for scheduling (default: UTC) |
| `active` | Boolean | Whether the rule is enabled |
| `createdBy` | String | Admin user ID |
| `createdAt` | Instant | Creation timestamp |
| `updatedAt` | Instant | Last update timestamp |
| `lastExecutedAt` | Instant | Last execution timestamp |
| `executionCount` | Integer | Total number of executions |
| `nextExecutionTime` | Instant | Next scheduled execution time |

---

## 📋 Enums

### NotificationStatus
| Value | Description |
|-------|-------------|
| `PENDING` | Notification created but not yet sent |
| `SENT` | Notification sent to channel provider |
| `DELIVERED` | Notification delivered to recipient's device |
| `READ` | Notification opened/read by recipient |
| `FAILED` | Notification delivery failed |

### NotificationPriority
| Value | Description |
|-------|-------------|
| `LOW` | Low priority, routine information |
| `NORMAL` | Standard priority notifications |
| `HIGH` | High priority, requires attention |
| `CRITICAL` | Critical priority, immediate action required |

### NotificationType
| Value | Description |
|-------|-------------|
| `ALERT` | Safety and security alerts |
| `REMINDER` | Medication and appointment reminders |
| `SYSTEM` | System updates and general notifications |
| `EMERGENCY` | Critical emergency alerts |

### NotificationChannel
| Value | Description |
|-------|-------------|
| `SMS` | Text message notifications |
| `EMAIL` | Email notifications |
| `PUSH` | Mobile push notifications |
| `IN_APP` | In-application notifications |

### ScheduleType
| Value | Description |
|-------|-------------|
| `INTERVAL` | Fixed interval between executions |
| `CRON` | Cron expression-based scheduling |
| `ONE_TIME` | Single execution at specific time |

---

## 🎯 Dynamic Scheduler Examples

### Example Use Cases

| Schedule Name | Target | Frequency | Use Case |
|--------------|--------|-----------|----------|
| Hydration Reminder | PATIENT | Every 3 hours | Keep patients hydrated |
| Caregiver Report | CAREGIVER | Daily at 6 PM | Daily patient summary |
| Weekly Check-in | PATIENT | Thursdays 10 AM | Wellness survey |
| Medication Morning | PATIENT | Daily 8 AM | Morning medication |
| Doctor Rounds | DOCTOR | Daily 9 AM | Patient status summary |

### Create Interval Schedule

```json
{
  "name": "Hydration Reminder",
  "description": "Remind patients to drink water every 3 hours",
  "targetRole": "PATIENT",
  "targetUserIds": [],
  "titleTemplate": "Stay Hydrated 💧",
  "messageTemplate": "Hi {{firstName}}, have you had water in the last hour? Stay hydrated!",
  "type": "REMINDER",
  "priority": "NORMAL",
  "channels": ["IN_APP"],
  "scheduleType": "INTERVAL",
  "intervalMinutes": 180,
  "startDate": "2026-02-22T08:00:00Z",
  "endDate": "2026-12-31T23:59:59Z",
  "timezone": "UTC",
  "active": true
}
```

### Create Cron Schedule

```json
{
  "name": "Daily Mood Check",
  "description": "Daily check-in reminder",
  "targetRole": "PATIENT",
  "titleTemplate": "Daily Check-in 📝",
  "messageTemplate": "How are you feeling today, {{firstName}}?",
  "type": "REMINDER",
  "priority": "NORMAL",
  "channels": ["IN_APP", "EMAIL"],
  "scheduleType": "CRON",
  "cronExpression": "0 0 9 * * *",
  "startDate": "2026-02-22T00:00:00Z",
  "timezone": "America/New_York",
  "active": true
}
```

---

## 📨 RabbitMQ Integration

### Configuration
- **Exchange**: `alert.notifications`
- **Queue**: `notification.queue`
- **DLX Exchange**: `alert.notifications.dlx`
- **DLQ**: `notification.queue.dlq`

### Event Payload Structure

```json
{
  "eventId": "uuid",
  "eventType": "AlertCreated|NotificationRequested",
  "source": "safety-alert-engine",
  "timestamp": "2026-02-21T14:30:00Z",
  "payload": {
    "alertId": "uuid",
    "patientId": "uuid",
    "recipientId": "uuid",
    "severity": "CRITICAL|HIGH|MEDIUM|LOW",
    "ruleCode": "FALL_DETECTED|WANDERING|...",
    "message": "Pre-formatted message content",
    "channel": "SMS|EMAIL|PUSH|IN_APP",
    "metadata": {
      "additionalKey": "value"
    }
  }
}
```

---

## 🔧 Build & Run

### Build
```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Package
mvn clean package -DskipTests

# Build Docker image
docker build -t alzcare/notification:latest .
```

### Run

**Local Development**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Docker**
```bash
docker run -p 8004:8004 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/notification_db \
  -e SPRING_RABBITMQ_HOST=localhost \
  alzcare/notification:latest
```

---

## ⚙️ Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Application port | 8004 |
| `SPRING_PROFILES_ACTIVE` | Active profile | dev |
| `SPRING_DATA_MONGODB_URI` | MongoDB connection URI | mongodb://localhost:27017/notification_db |
| `SPRING_RABBITMQ_HOST` | RabbitMQ host | localhost |
| `SPRING_RABBITMQ_PORT` | RabbitMQ port | 5672 |
| `SPRING_RABBITMQ_USERNAME` | RabbitMQ username | guest |
| `SPRING_RABBITMQ_PASSWORD` | RabbitMQ password | guest |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | Eureka server URL | http://localhost:8761/eureka |
| `NOTIFICATION_SCHEDULER_ENABLED` | Enable scheduler | true |
| `NOTIFICATION_SCHEDULER_RELOAD_ON_STARTUP` | Load rules on startup | true |

---

## 🏗️ Internal Architecture

### Service Layers

```
┌─────────────────────────────────────────────────────────────────┐
│  CONTROLLER LAYER                                                │
│  ├── NotificationController      (REST API for notifications)   │
│  ├── NotificationScheduleController (Schedule CRUD)             │
│  └── NotificationTemplateController (Template management)       │
├─────────────────────────────────────────────────────────────────┤
│  SERVICE LAYER                                                   │
│  ├── NotificationProcessor       (Event processing)             │
│  ├── NotificationRoutingService  (Channel routing)              │
│  ├── DynamicSchedulerService     (Schedule execution)           │
│  ├── ScheduleExecutionService    (Rule execution)               │
│  ├── TargetUserResolver          (User resolution via Feign)    │
│  ├── NotificationRetryScheduler  (Failed retry every 60s)       │
│  └── RetryableNotificationService (Retry logic)                 │
├─────────────────────────────────────────────────────────────────┤
│  CHANNEL LAYER (Service Interface)                               │
│  ├── InAppChannelService         (MongoDB delivery)             │
│  ├── SmsChannelService           (Twilio-ready)                 │
│  ├── EmailChannelService         (SendGrid-ready)               │
│  └── PushChannelService          (Firebase-ready)               │
├─────────────────────────────────────────────────────────────────┤
│  INTEGRATION LAYER                                               │
│  ├── NotificationEventListener   (RabbitMQ consumer)            │
│  ├── IdentityServiceClientFeign  (Identity Service client)      │
│  └── ScheduleBootstrapService    (Startup schedule loader)      │
└─────────────────────────────────────────────────────────────────┘
```

### Scheduler Architecture Detail

```
┌──────────────────────────────────────────────────────────────────────┐
│                     DYNAMIC NOTIFICATION SCHEDULER                    │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌──────────────┐    ┌──────────────┐    ┌─────────────────────────┐ │
│  │ Admin API    │───►│ Schedule     │───►│ MongoDB                 │ │
│  │ (CRUD)       │    │ Repository   │    │ (Rules)                 │ │
│  └──────────────┘    └──────────────┘    └─────────────────────────┘ │
│                             │                                         │
│                             ▼                                         │
│                    ┌──────────────────┐                               │
│                    │ Dynamic          │                               │
│                    │ Task Scheduler   │                               │
│                    └────────┬─────────┘                               │
│                             │                                         │
│                             ▼                                         │
│                    ┌──────────────────┐                               │
│                    │ Schedule         │                               │
│                    │ Execution        │                               │
│                    └────────┬─────────┘                               │
│                             │                                         │
│                             ▼                                         │
│            ┌──────────────────────────────────────────┐              │
│            │  1. Resolve target users (OpenFeign)    │              │
│            │  2. Process templates (variables)       │              │
│            │  3. Create notifications                │              │
│            │  4. Route to channels                   │              │
│            └──────────────────────────────────────────┘              │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 🧪 Testing

Integration tests use Testcontainers for:
- MongoDB
- RabbitMQ

```bash
mvn test
```

---

## 🔗 Inter-Service Communication

### OpenFeign Integration

The notification service uses **OpenFeign** for inter-service communication with the Identity Service:

```java
@FeignClient(name = "identity-service")
public interface IdentityServiceFeignClient {
    @GetMapping("/api/v1/admin/users")
    PaginatedUserResponse getUsers(@RequestParam(required = false) String role, 
                                   @RequestParam(required = false) Boolean active);
    
    @GetMapping("/api/v1/admin/users/{id}")
    IdentityUserResponse getUserById(@PathVariable("id") String userId);
}
```

**Features:**
- **Service Discovery**: Eureka automatically resolves `identity-service` instances
- **Load Balancing**: Client-side load balancing
- **Error Handling**: Automatic retry and fallback mechanisms

---

## 📦 Dependencies

- Spring Boot 3.2.2
- Spring Data MongoDB
- Spring AMQP (RabbitMQ)
- Spring Cloud Netflix Eureka Client
- Spring Cloud Config Client
- Spring Cloud OpenFeign
- MapStruct for mapping
- Lombok for boilerplate reduction
- Swagger/OpenAPI for API documentation

---

## License

Copyright © 2025 AlzCare Platform
