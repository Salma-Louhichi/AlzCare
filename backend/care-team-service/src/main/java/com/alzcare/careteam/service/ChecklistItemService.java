package com.alzcare.careteam.service;

import com.alzcare.careteam.dto.ChecklistGroupResponse;
import com.alzcare.careteam.dto.GenerateChecklistRequest;
import com.alzcare.careteam.entity.ChecklistItem;
import com.alzcare.careteam.entity.DoctorAssignment;
import com.alzcare.careteam.repository.ChecklistItemRepository;
import com.alzcare.careteam.repository.DoctorAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChecklistItemService {

    private final ChecklistItemRepository repo;
    private final DoctorAssignmentRepository doctorAssignmentRepository;
    private final NotificationIntegrationService notificationIntegrationService;

    public ChecklistItemService(
            ChecklistItemRepository repo,
            DoctorAssignmentRepository doctorAssignmentRepository,
            NotificationIntegrationService notificationIntegrationService) {
        this.repo = repo;
        this.doctorAssignmentRepository = doctorAssignmentRepository;
        this.notificationIntegrationService = notificationIntegrationService;
    }

    @Transactional
    public ChecklistItem create(ChecklistItem item) {
        item.setChecklistGeneratedAt(LocalDateTime.now());
        if (item.getStatus() == null) {
            item.setStatus(ChecklistItem.ItemStatus.PENDING);
        }
        if (item.getChecklistStatus() == null) {
            item.setChecklistStatus(ChecklistItem.ChecklistStatus.PENDING);
        }
        ChecklistItem saved = repo.save(item);
        refreshAggregateChecklistStatus(
                saved.getDoctorId(), saved.getPatientId(), saved.getDate());
        return saved;
    }

    public ChecklistItem getById(UUID id) {
        return repo.findById(id).orElseThrow();
    }

    public List<ChecklistItem> getByDoctor(String doctorId) {
        return repo.findByDoctorId(doctorId);
    }

    /**
     * If {@code doctorId} is omitted, resolves the active doctor for the patient (Angular / Postman friendly).
     */
    public List<ChecklistItem> getByDoctorPatientDate(
            String doctorId, String patientId, LocalDate date) {
        String resolved = doctorId;
        if (resolved == null || resolved.isBlank()) {
            resolved = doctorAssignmentRepository
                    .findByPatientIdAndStatus(patientId, DoctorAssignment.DoctorAssignmentStatus.ACTIVE)
                    .map(DoctorAssignment::getDoctorId)
                    .orElse(null);
        }
        if (resolved == null) {
            return List.of();
        }
        return repo.findByDoctorIdAndPatientIdAndDate(resolved, patientId, date);
    }

    @Transactional
    public ChecklistItem complete(UUID id, String completedBy) {
        ChecklistItem item = repo.findById(id).orElseThrow();
        item.setStatus(ChecklistItem.ItemStatus.COMPLETED);
        item.setCompletedBy(completedBy);
        item.setCompletedAt(LocalDateTime.now());
        ChecklistItem saved = repo.save(item);
        refreshAggregateChecklistStatus(
                saved.getDoctorId(), saved.getPatientId(), saved.getDate());
        notificationIntegrationService.notifyDoctorChecklistItemCompleted(saved, completedBy);
        return saved;
    }

    @Transactional
    public ChecklistItem assignToCaregiver(UUID id, String caregiverId) {
        ChecklistItem item = repo.findById(id).orElseThrow();
        item.setAssignedCaregiverId(caregiverId);
        item.setStatus(ChecklistItem.ItemStatus.ASSIGNED);
        ChecklistItem saved = repo.save(item);
        refreshAggregateChecklistStatus(
                saved.getDoctorId(), saved.getPatientId(), saved.getDate());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        ChecklistItem item = repo.findById(id).orElseThrow();
        String doctorId = item.getDoctorId();
        String patientId = item.getPatientId();
        LocalDate date = item.getDate();
        repo.deleteById(id);
        refreshAggregateChecklistStatus(doctorId, patientId, date);
    }

    public List<ChecklistItem> findAll(String patientId, String doctorId, String assignedTo, String date, Boolean completed) {
        List<ChecklistItem> all = repo.findAll();

        return all.stream()
                .filter(item -> patientId == null || item.getPatientId().equals(patientId))
                .filter(item -> doctorId == null || item.getDoctorId().equals(doctorId))
                .filter(item -> assignedTo == null
                        || (item.getAssignedCaregiverId() != null && item.getAssignedCaregiverId().equals(assignedTo)))
                .filter(item -> date == null || item.getDate().toString().equals(date))
                .filter(item -> completed == null
                        || (completed
                        ? item.getStatus() == ChecklistItem.ItemStatus.COMPLETED
                        : item.getStatus() != ChecklistItem.ItemStatus.COMPLETED))
                .toList();
    }

    /**
     * FR6 — generate daily checklist (stub items stand in for other axes until integrated).
     */
    @Transactional
    public List<ChecklistItem> generate(GenerateChecklistRequest req) {
        List<ChecklistItem> existing = repo.findByDoctorIdAndPatientIdAndDate(
                req.doctorId(), req.patientId(), req.date());
        if (!existing.isEmpty()) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        List<ChecklistItem> toSave = new ArrayList<>();

        if (req.items() != null && !req.items().isEmpty()) {
            int idx = 0;
            for (GenerateChecklistRequest.ItemSeed seed : req.items()) {
                idx++;
                toSave.add(ChecklistItem.builder()
                        .doctorId(req.doctorId())
                        .patientId(req.patientId())
                        .date(req.date())
                        .itemOrder(seed.itemOrder() != null ? seed.itemOrder() : idx)
                        .description(seed.description())
                        .priority(seed.priority() != null ? seed.priority() : ChecklistItem.Priority.MEDIUM)
                        .category(seed.category() != null ? seed.category() : ChecklistItem.Category.GENERAL)
                        .status(ChecklistItem.ItemStatus.PENDING)
                        .sourceAxisReference(seed.sourceAxisReference())
                        .checklistGeneratedAt(now)
                        .checklistStatus(ChecklistItem.ChecklistStatus.PENDING)
                        .build());
            }
        } else {
            toSave.add(stubItem(req.doctorId(), req.patientId(), req.date(), now, 1,
                    "Review medication plan and adherence",
                    ChecklistItem.Category.MEDICATION, ChecklistItem.Priority.HIGH, "AXIS1_STUB"));
            toSave.add(stubItem(req.doctorId(), req.patientId(), req.date(), now, 2,
                    "Review recent incidents / behavior notes",
                    ChecklistItem.Category.INCIDENT, ChecklistItem.Priority.MEDIUM, "AXIS2_STUB"));
            toSave.add(stubItem(req.doctorId(), req.patientId(), req.date(), now, 3,
                    "Review latest cognitive screening results",
                    ChecklistItem.Category.COGNITIVE_TEST, ChecklistItem.Priority.MEDIUM, "AXIS4_STUB"));
        }

        List<ChecklistItem> saved = repo.saveAll(toSave);
        refreshAggregateChecklistStatus(req.doctorId(), req.patientId(), req.date());
        return saved;
    }

    private static ChecklistItem stubItem(
            String doctorId, String patientId, LocalDate date, LocalDateTime now,
            int order, String description, ChecklistItem.Category category,
            ChecklistItem.Priority priority, String sourceRef) {
        return ChecklistItem.builder()
                .doctorId(doctorId)
                .patientId(patientId)
                .date(date)
                .itemOrder(order)
                .description(description)
                .priority(priority)
                .category(category)
                .status(ChecklistItem.ItemStatus.PENDING)
                .sourceAxisReference(sourceRef)
                .checklistGeneratedAt(now)
                .checklistStatus(ChecklistItem.ChecklistStatus.PENDING)
                .build();
    }

    /**
     * Checklist-level status: COMPLETED only if every item is COMPLETED (PDF module 3.2).
     */
    @Transactional
    public void refreshAggregateChecklistStatus(String doctorId, String patientId, LocalDate date) {
        List<ChecklistItem> group = repo.findByDoctorIdAndPatientIdAndDate(doctorId, patientId, date);
        if (group.isEmpty()) {
            return;
        }
        boolean allCompleted = group.stream()
                .allMatch(i -> i.getStatus() == ChecklistItem.ItemStatus.COMPLETED);
        ChecklistItem.ChecklistStatus agg = allCompleted
                ? ChecklistItem.ChecklistStatus.COMPLETED
                : ChecklistItem.ChecklistStatus.PENDING;
        for (ChecklistItem i : group) {
            i.setChecklistStatus(agg);
        }
        repo.saveAll(group);
    }

    public List<ChecklistGroupResponse> getGroupedByDoctor(String doctorId) {
        List<ChecklistItem> all = repo.findByDoctorId(doctorId);
        return all.stream()
                .collect(Collectors.groupingBy(i -> i.getPatientId() + "|" + i.getDate()))
                .values()
                .stream()
                .map(items -> {
                    List<ChecklistItem> sorted = items.stream()
                            .sorted(Comparator.comparing(
                                    ChecklistItem::getItemOrder,
                                    Comparator.nullsLast(Integer::compareTo)))
                            .toList();
                    ChecklistItem first = sorted.get(0);
                    boolean allDone = sorted.stream()
                            .allMatch(i -> i.getStatus() == ChecklistItem.ItemStatus.COMPLETED);
                    String agg = allDone ? "COMPLETED" : "PENDING";
                    return new ChecklistGroupResponse(
                            first.getDoctorId(),
                            first.getPatientId(),
                            first.getDate(),
                            agg,
                            sorted);
                })
                .sorted(Comparator.comparing(ChecklistGroupResponse::date).reversed())
                .toList();
    }
}
