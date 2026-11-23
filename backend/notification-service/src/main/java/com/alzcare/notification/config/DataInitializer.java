package com.alzcare.notification.config;

import com.alzcare.notification.entities.NotificationTemplate;
import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationType;
import com.alzcare.notification.repositories.NotificationTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Data initializer for development environment.
 * Creates sample notification templates if the database is empty.
 */
@Component
@Profile("dev")
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final NotificationTemplateRepository templateRepository;

    public DataInitializer(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing notification templates for dev environment...");

        // Check if templates already exist
        long count = templateRepository.count();
        if (count > 0) {
            log.info("Templates already exist (count: {}), skipping initialization", count);
            return;
        }

        // Create sample templates
        createAlertCriticalTemplate();
        createAlertHighTemplate();
        createReminderMedicationTemplate();
        createSystemNotificationTemplate();
        createEmergencyTemplate();

        log.info("Notification templates initialized successfully");
    }

    private void createAlertCriticalTemplate() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id("ALERT_CRITICAL")
                .name("Critical Alert")
                .type(NotificationType.EMERGENCY)
                .channel(NotificationChannel.SMS)
                .subjectTemplate("CRITICAL: {{alertType}} Alert")
                .contentTemplate("CRITICAL ALERT for patient {{patientName}}: {{message}}. Immediate attention required!")
                .description("Used for critical safety alerts requiring immediate attention via SMS and Email")
                .language("en")
                .variables(Arrays.asList("patientName", "alertType", "message", "location"))
                .build();

        templateRepository.save(template);
        log.info("Created template: ALERT_CRITICAL (SMS)");

        // Also create email version
        NotificationTemplate emailTemplate = NotificationTemplate.builder()
                .id("ALERT_CRITICAL_EMAIL")
                .name("Critical Alert - Email")
                .type(NotificationType.EMERGENCY)
                .channel(NotificationChannel.EMAIL)
                .subjectTemplate("Critical Safety Alert - {{alertType}}")
                .contentTemplate("<h2>Critical Alert</h2><p>Patient: <strong>{{patientName}}</strong></p><p>Alert Type: {{alertType}}</p><p>Message: {{message}}</p><p>Location: {{location}}</p><p>Time: {{timestamp}}</p>")
                .description("Email version of critical alerts")
                .language("en")
                .variables(Arrays.asList("patientName", "alertType", "message", "location", "timestamp"))
                .build();

        templateRepository.save(emailTemplate);
        log.info("Created template: ALERT_CRITICAL (EMAIL)");
    }

    private void createAlertHighTemplate() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id("ALERT_HIGH")
                .name("High Priority Alert")
                .type(NotificationType.ALERT)
                .channel(NotificationChannel.EMAIL)
                .subjectTemplate("High Priority Alert: {{alertType}}")
                .contentTemplate("<h3>High Priority Alert</h3><p>Patient: {{patientName}}</p><p>Alert: {{alertType}}</p><p>Details: {{message}}</p>")
                .description("Used for high priority alerts sent via email")
                .language("en")
                .variables(Arrays.asList("patientName", "alertType", "message"))
                .build();

        templateRepository.save(template);
        log.info("Created template: ALERT_HIGH (EMAIL)");
    }

    private void createReminderMedicationTemplate() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id("REMINDER_MEDICATION")
                .name("Medication Reminder")
                .type(NotificationType.REMINDER)
                .channel(NotificationChannel.PUSH)
                .subjectTemplate("Medication Reminder")
                .contentTemplate("Time to take {{medicationName}} - {{dosage}}")
                .description("Push notification for medication reminders")
                .language("en")
                .variables(Arrays.asList("patientName", "medicationName", "dosage", "time"))
                .build();

        templateRepository.save(template);
        log.info("Created template: REMINDER_MEDICATION (PUSH)");

        // Also create in-app version
        NotificationTemplate inAppTemplate = NotificationTemplate.builder()
                .id("REMINDER_MEDICATION_IN_APP")
                .name("Medication Reminder - In App")
                .type(NotificationType.REMINDER)
                .channel(NotificationChannel.IN_APP)
                .subjectTemplate("Medication Reminder")
                .contentTemplate("{{patientName}}, it's time to take your medication: {{medicationName}} ({{dosage}}) at {{time}}")
                .description("In-app notification for medication reminders")
                .language("en")
                .variables(Arrays.asList("patientName", "medicationName", "dosage", "time"))
                .build();

        templateRepository.save(inAppTemplate);
        log.info("Created template: REMINDER_MEDICATION (IN_APP)");
    }

    private void createSystemNotificationTemplate() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id("SYSTEM_NOTIFICATION")
                .name("System Notification")
                .type(NotificationType.SYSTEM)
                .channel(NotificationChannel.IN_APP)
                .subjectTemplate("System Update")
                .contentTemplate("{{message}}")
                .description("General system notifications and updates")
                .language("en")
                .variables(Arrays.asList("message", "action"))
                .build();

        templateRepository.save(template);
        log.info("Created template: SYSTEM_NOTIFICATION (IN_APP)");
    }

    private void createEmergencyTemplate() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id("EMERGENCY_BUTTON")
                .name("Emergency Button Alert")
                .type(NotificationType.EMERGENCY)
                .channel(NotificationChannel.SMS)
                .subjectTemplate("EMERGENCY BUTTON PRESSED")
                .contentTemplate("EMERGENCY: {{patientName}} has pressed the emergency button at {{location}}. Time: {{timestamp}}")
                .description("Immediate alert when patient presses emergency button")
                .language("en")
                .variables(Arrays.asList("patientName", "location", "timestamp"))
                .build();

        templateRepository.save(template);
        log.info("Created template: EMERGENCY_BUTTON (SMS)");
    }
}
