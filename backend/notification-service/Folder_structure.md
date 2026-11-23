Here's a breakdown of every folder and file in the notification service:

---

## 📁 Folder Structure Overview

```
notification-service/src/main/java/com/alzcare/notification/
├── NotificationServiceApplication.java     # Main Spring Boot app entry point
├── client/                                 # OpenFeign clients (external API calls)
│   ├── dto/
│   │   ├── IdentityUserResponse.java       # DTO: User data from Identity Service
│   │   └── PaginatedUserResponse.java      # DTO: Paginated list wrapper
│   └── IdentityServiceFeignClient.java     # Feign client interface for Identity Service
├── config/                                 # Configuration classes
│   ├── DataInitializer.java                # Creates default templates on startup (dev only)
│   ├── MongoConfig.java                    # MongoDB auditing & repository config
│   ├── RabbitMQConfig.java                 # Queues, exchanges, bindings for RabbitMQ
│   ├── ScheduleBootstrapService.java       # Loads schedules from DB on startup
│   └── SecurityConfig.java                 # Spring Security configuration
├── controller/                             # REST API endpoints
│   ├── NotificationController.java         # CRUD for notifications
│   ├── NotificationScheduleController.java # CRUD for schedules
│   └── NotificationTemplateController.java # CRUD for templates
├── dto/                                    # Data Transfer Objects
│   ├── request/                            # Incoming request bodies
│   │   ├── CreateScheduleRequest.java      # Create schedule JSON body
│   │   ├── NotificationRequest.java        # Send notification JSON body
│   │   ├── NotificationTemplateRequest.java# Create template JSON body
│   │   └── UpdateScheduleRequest.java      # Update schedule JSON body
│   ├── response/                           # Outgoing response bodies
│   │   ├── NotificationCountResponse.java  # Unread count response
│   │   ├── NotificationResponse.java       # Notification data response
│   │   ├── NotificationTemplateResponse.java# Template data response
│   │   └── ScheduleResponse.java           # Schedule data response
│   ├── NotificationEvent.java              # RabbitMQ event wrapper
│   ├── NotificationPayload.java            # Event payload data
│   └── UserDto.java                        # Internal user DTO
├── entities/                               # MongoDB document models
│   ├── Notification.java                   # Main notification document
│   ├── NotificationTemplate.java           # Reusable template document
│   └── ScheduledNotificationRule.java      # Schedule rule document
├── enums/                                  # Constants/enumerations
│   ├── NotificationChannel.java            # SMS, EMAIL, PUSH, IN_APP
│   ├── NotificationPriority.java           # LOW, NORMAL, HIGH, CRITICAL
│   ├── NotificationStatus.java             # PENDING, SENT, DELIVERED, READ, FAILED
│   ├── NotificationType.java               # ALERT, REMINDER, SYSTEM, EMERGENCY
│   └── ScheduleType.java                   # INTERVAL, CRON, ONE_TIME
├── exception/                              # Custom exceptions
│   ├── GlobalExceptionHandler.java         # Global @ControllerAdvice for errors
│   ├── NotificationChannelException.java   # Channel delivery failure
│   ├── ResourceNotFoundException.java      # Entity not found (404)
│   └── TargetResolutionException.java      # User resolution failure
├── mapper/                                 # Object mappers
│   └── NotificationMapper.java             # MapStruct: Entity ↔ DTO conversions
├── repositories/                           # MongoDB data access
│   ├── NotificationRepository.java         # Notification queries
│   ├── NotificationScheduleRepository.java # Schedule queries
│   └── NotificationTemplateRepository.java # Template queries
└── service/                                # Business logic
    ├── channel/                            # Delivery channel implementations
    │   ├── ChannelServiceFactory.java      # Factory to get channel by type
    │   ├── EmailChannelService.java        # Email delivery (mock)
    │   ├── InAppChannelService.java        # In-app delivery (MongoDB)
    │   ├── NotificationChannelService.java # Interface for all channels
    │   ├── PushChannelService.java         # Push delivery (mock)
    │   └── SmsChannelService.java          # SMS delivery (mock)
    ├── dynamic/
    │   └── DynamicSchedulerService.java    # Core scheduler engine (TaskScheduler)
    ├── execution/
    │   └── ScheduleExecutionService.java   # Executes schedule rules
    ├── messaging/
    │   └── NotificationEventListener.java  # RabbitMQ listener
    ├── target/
    │   ├── IdentityServiceClientFeign.java # Wrapper for Feign client
    │   └── TargetUserResolver.java         # Resolves users by role/ID
    ├── NotificationProcessor.java          # Processes RabbitMQ events
    ├── NotificationRetryScheduler.java     # Retries failed notifications (60s)
    ├── NotificationRoutingService.java     # Routes to correct channel
    └── RetryableNotificationService.java   # Retry logic & tracking
```

---

## 🎯 Key Files by Purpose

| Purpose | Key Files |
|---------|-----------|
| **Entry Point** | `NotificationServiceApplication.java` |
| **REST API** | `NotificationController`, `NotificationScheduleController`, `NotificationTemplateController` |
| **Event Consumer** | `NotificationEventListener.java` (RabbitMQ) |
| **Core Scheduler** | `DynamicSchedulerService.java`, `ScheduleExecutionService.java` |
| **Channel Delivery** | `InAppChannelService.java` (active), `Sms/Email/Push` (mock) |
| **External Calls** | `IdentityServiceFeignClient.java`, `IdentityServiceClientFeign.java` |
| **Data Models** | `Notification.java`, `ScheduledNotificationRule.java`, `NotificationTemplate.java` |
| **Startup Tasks** | `DataInitializer.java` (templates), `ScheduleBootstrapService.java` (schedules) |