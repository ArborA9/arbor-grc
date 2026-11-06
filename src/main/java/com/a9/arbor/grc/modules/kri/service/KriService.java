package com.a9.arbor.grc.modules.kri.service;

import com.a9.arbor.grc.common.validation.DateRules;
import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.kri.domain.Kri;
import com.a9.arbor.grc.modules.kri.domain.KriDirection;
import com.a9.arbor.grc.modules.kri.domain.KriFrequency;
import com.a9.arbor.grc.modules.kri.domain.KriRag;
import com.a9.arbor.grc.modules.kri.domain.KriReading;
import com.a9.arbor.grc.modules.kri.domain.KriScoring;
import com.a9.arbor.grc.modules.kri.domain.KriStatus;
import com.a9.arbor.grc.modules.kri.domain.KriTrend;
import com.a9.arbor.grc.modules.kri.repo.KriReadingRepository;
import com.a9.arbor.grc.modules.kri.repo.KriRepository;
import com.a9.arbor.grc.modules.kri.web.dto.CreateKriRequest;
import com.a9.arbor.grc.modules.kri.web.dto.KriReadingResponse;
import com.a9.arbor.grc.modules.kri.web.dto.KriResponse;
import com.a9.arbor.grc.modules.kri.web.dto.RecordKriValueRequest;
import com.a9.arbor.grc.modules.kri.web.dto.UpdateKriRequest;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.risk.repo.RiskRecordRepository;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import com.a9.arbor.grc.modules.task.service.MyTasksService;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KriService {

    private final KriRepository kriRepository;
    private final KriReadingRepository readingRepository;
    private final KriIdGenerator idGenerator;
    private final UserRepository userRepository;
    private final RiskRecordRepository riskRepository;
    private final WorkTaskRepository workTaskRepository;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;
    private final ModuleLicensingService moduleLicensingService;

    public KriService(KriRepository kriRepository,
                      KriReadingRepository readingRepository,
                      KriIdGenerator idGenerator,
                      UserRepository userRepository,
                      RiskRecordRepository riskRepository,
                      WorkTaskRepository workTaskRepository,
                      NotificationService notificationService,
                      AuditTrailService auditTrailService,
                      ModuleLicensingService moduleLicensingService) {
        this.kriRepository = kriRepository;
        this.readingRepository = readingRepository;
        this.idGenerator = idGenerator;
        this.userRepository = userRepository;
        this.riskRepository = riskRepository;
        this.workTaskRepository = workTaskRepository;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
        this.moduleLicensingService = moduleLicensingService;
    }

    @Transactional(readOnly = true)
    public List<KriResponse> list(User user) {
        requireErm();
        List<Kri> rows;
        if (RoleAccess.isContributorOnly(user)) {
            rows = kriRepository.findByOwnerUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), KriStatus.ACTIVE);
        } else {
            rows = kriRepository.findAllByOrderByUpdatedAtDesc();
        }
        return rows.stream().map(KriResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public KriResponse get(User user, long id) {
        requireErm();
        Kri kri = requireVisible(user, id);
        List<KriReadingResponse> readings = readingRepository
                .findByKriIdOrderByRecordedAtDescIdDesc(kri.getId()).stream()
                .limit(20)
                .map(KriReadingResponse::from)
                .toList();
        return KriResponse.from(kri, readings);
    }

    @Transactional
    public KriResponse create(User admin, CreateKriRequest req) {
        requireErm();
        requireAdmin(admin);
        validateThresholds(req.direction(), req.greenThreshold(), req.amberThreshold(), req.redThreshold());
        requireUser(req.ownerUserId());
        if (req.linkedRiskId() != null) {
            requireRisk(req.linkedRiskId());
        }

        Kri kri = new Kri();
        kri.setDisplayId(idGenerator.nextDisplayId());
        kri.setName(req.name().trim());
        kri.setLinkedRiskId(req.linkedRiskId());
        kri.setMetricDescription(blankToNull(req.metricDescription()));
        kri.setUnit(blankToNull(req.unit()));
        kri.setFrequency(req.frequency());
        kri.setOwnerUserId(req.ownerUserId());
        kri.setDirection(req.direction());
        kri.setGreenThreshold(req.greenThreshold());
        kri.setAmberThreshold(req.amberThreshold());
        kri.setRedThreshold(req.redThreshold());
        DateRules.requireActionDueDateIfPresent(req.nextDueDate(), "Next due date");
        LocalDate due = req.nextDueDate() != null ? req.nextDueDate() : advance(LocalDate.now(), req.frequency());
        kri.setNextDueDate(due);
        kri.setStatus(KriStatus.ACTIVE);
        kri.setCreatedByUserId(admin.getId());
        kriRepository.save(kri);

        audit(admin, AuditActionType.CREATED, kri);
        return KriResponse.from(kri);
    }

    @Transactional
    public KriResponse update(User admin, long id, UpdateKriRequest req) {
        requireErm();
        requireAdmin(admin);
        Kri kri = require(id);
        if (req.name() != null && !req.name().isBlank()) {
            kri.setName(req.name().trim());
        }
        if (req.linkedRiskId() != null) {
            if (req.linkedRiskId() == 0L) {
                kri.setLinkedRiskId(null);
            } else {
                requireRisk(req.linkedRiskId());
                kri.setLinkedRiskId(req.linkedRiskId());
            }
        }
        if (req.metricDescription() != null) {
            kri.setMetricDescription(blankToNull(req.metricDescription()));
        }
        if (req.unit() != null) {
            kri.setUnit(blankToNull(req.unit()));
        }
        if (req.frequency() != null) {
            kri.setFrequency(req.frequency());
        }
        if (req.ownerUserId() != null) {
            requireUser(req.ownerUserId());
            kri.setOwnerUserId(req.ownerUserId());
        }
        KriDirection direction = req.direction() != null ? req.direction() : kri.getDirection();
        BigDecimal green = req.greenThreshold() != null ? req.greenThreshold() : kri.getGreenThreshold();
        BigDecimal amber = req.amberThreshold() != null ? req.amberThreshold() : kri.getAmberThreshold();
        BigDecimal red = req.redThreshold() != null ? req.redThreshold() : kri.getRedThreshold();
        if (req.direction() != null || req.greenThreshold() != null
                || req.amberThreshold() != null || req.redThreshold() != null) {
            validateThresholds(direction, green, amber, red);
            kri.setDirection(direction);
            kri.setGreenThreshold(green);
            kri.setAmberThreshold(amber);
            kri.setRedThreshold(red);
            if (kri.getCurrentValue() != null) {
                kri.setRag(KriScoring.rag(direction, kri.getCurrentValue(), green, amber, red));
            }
        }
        if (req.nextDueDate() != null) {
            DateRules.requireActionDueDateIfChanged(kri.getNextDueDate(), req.nextDueDate(), "Next due date");
            kri.setNextDueDate(req.nextDueDate());
            kri.setDueNotifiedAt(null);
            kri.setOverdueNotifiedAt(null);
        }
        if (req.status() != null) {
            kri.setStatus(req.status());
        }
        kriRepository.save(kri);
        audit(admin, AuditActionType.UPDATED, kri);
        return get(admin, kri.getId());
    }

    @Transactional
    public KriResponse recordValue(User user, long id, RecordKriValueRequest req) {
        requireErm();
        Kri kri = requireVisible(user, id);
        if (kri.getStatus() != KriStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "KRI is inactive");
        }
        boolean admin = user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.OWNER);
        if (!admin && !user.getId().equals(kri.getOwnerUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only KRI owner or Admin may enter values");
        }

        BigDecimal value = req.value();
        KriRag rag = KriScoring.rag(kri.getDirection(), value,
                kri.getGreenThreshold(), kri.getAmberThreshold(), kri.getRedThreshold());
        KriTrend trend = KriScoring.trend(kri.getDirection(), kri.getCurrentValue(), value);

        KriReading reading = new KriReading();
        reading.setKriId(kri.getId());
        reading.setValue(value);
        reading.setRag(rag);
        reading.setNotes(blankToNull(req.notes()));
        reading.setRecordedByUserId(user.getId());
        readingRepository.save(reading);

        kri.setPreviousValue(kri.getCurrentValue());
        kri.setCurrentValue(value);
        kri.setRag(rag);
        kri.setTrend(trend);
        kri.setLastMeasuredAt(Instant.now());
        kri.setNextDueDate(advance(LocalDate.now(), kri.getFrequency()));
        kri.setDueNotifiedAt(null);
        kri.setOverdueNotifiedAt(null);
        if (rag != KriRag.RED) {
            kri.setRedAcknowledgedAt(null);
            kri.setRedAcknowledgedBy(null);
        }
        completeOpenTask(kri);
        kriRepository.save(kri);

        notifyBreach(kri, rag, value);
        audit(user, AuditActionType.UPDATED, kri);
        return get(user, kri.getId());
    }

    @Transactional
    public KriResponse acknowledgeRed(User admin, long id) {
        requireErm();
        requireAdmin(admin);
        Kri kri = require(id);
        if (kri.getRag() != KriRag.RED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "KRI is not in Red");
        }
        kri.setRedAcknowledgedAt(Instant.now());
        kri.setRedAcknowledgedBy(admin.getId());
        kriRepository.save(kri);
        notificationService.trigger(
                "KRI_RED_ACKNOWLEDGED",
                String.valueOf(kri.getId()),
                List.of(String.valueOf(kri.getOwnerUserId())),
                Map.of("displayId", kri.getDisplayId(), "name", kri.getName()));
        audit(admin, AuditActionType.UPDATED, kri);
        return get(admin, kri.getId());
    }

    /** Used by My Tasks submit. */
    @Transactional
    public KriResponse recordValueFromTask(User user, long kriId, RecordKriValueRequest req) {
        return recordValue(user, kriId, req);
    }

    @Transactional(readOnly = true)
    public Kri requireEntity(long id) {
        return require(id);
    }

    private void notifyBreach(Kri kri, KriRag rag, BigDecimal value) {
        if (rag == KriRag.AMBER) {
            List<String> recipients = new ArrayList<>();
            recipients.add(String.valueOf(kri.getOwnerUserId()));
            recipients.add("ADMIN");
            notificationService.trigger(
                    "KRI_AMBER_BREACH",
                    String.valueOf(kri.getId()),
                    recipients,
                    Map.of(
                            "displayId", kri.getDisplayId(),
                            "name", kri.getName(),
                            "value", value.toPlainString(),
                            "threshold", kri.getAmberThreshold().toPlainString()));
        } else if (rag == KriRag.RED) {
            List<String> recipients = new ArrayList<>();
            recipients.add(String.valueOf(kri.getOwnerUserId()));
            recipients.add("ADMIN");
            recipients.add("OWNER");
            notificationService.trigger(
                    "KRI_RED_BREACH",
                    String.valueOf(kri.getId()),
                    recipients,
                    Map.of(
                            "displayId", kri.getDisplayId(),
                            "name", kri.getName(),
                            "value", value.toPlainString(),
                            "threshold", kri.getRedThreshold().toPlainString()));
        }
    }

    private void completeOpenTask(Kri kri) {
        if (kri.getWorkTaskId() == null) {
            return;
        }
        workTaskRepository.findById(kri.getWorkTaskId()).ifPresent(task -> {
            if (task.getStatus() != WorkTaskStatus.SUBMITTED && task.getStatus() != WorkTaskStatus.CANCELLED) {
                task.setStatus(WorkTaskStatus.SUBMITTED);
                task.setSubmittedAt(Instant.now());
                workTaskRepository.save(task);
            }
        });
    }

    public void ensureDueTask(Kri kri) {
        WorkTask task = workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_KRI, MyTasksService.TYPE_KRI, kri.getId())
                .orElseGet(WorkTask::new);
        if (task.getId() != null && task.getStatus() == WorkTaskStatus.PENDING) {
            kri.setWorkTaskId(task.getId());
            return;
        }
        if (task.getId() != null && task.getStatus() == WorkTaskStatus.SUBMITTED) {
            task.setStatus(WorkTaskStatus.PENDING);
            task.setSubmittedAt(null);
        }
        task.setSourceModule(MyTasksService.MODULE_KRI);
        task.setSourceRecordType(MyTasksService.TYPE_KRI);
        task.setSourceRecordId(kri.getId());
        task.setTitle(kri.getDisplayId() + ": " + kri.getName());
        task.setDetail("KRI value due" + (kri.getUnit() != null ? " (" + kri.getUnit() + ")" : ""));
        task.setAssignedToUserId(kri.getOwnerUserId());
        task.setAssignedByUserId(kri.getCreatedByUserId());
        task.setStatus(WorkTaskStatus.PENDING);
        task.setDueDate(kri.getNextDueDate());
        workTaskRepository.save(task);
        kri.setWorkTaskId(task.getId());
    }

    public static LocalDate advance(LocalDate from, KriFrequency frequency) {
        return switch (frequency) {
            case DAILY -> from.plusDays(1);
            case WEEKLY -> from.plusWeeks(1);
            case MONTHLY -> from.plusMonths(1);
            case QUARTERLY -> from.plusMonths(3);
            case ANNUAL -> from.plusYears(1);
        };
    }

    private void validateThresholds(KriDirection direction, BigDecimal green, BigDecimal amber, BigDecimal red) {
        try {
            KriScoring.validateThresholds(direction, green, amber, red);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private void requireErm() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()
                || !moduleLicensingService.isEnabled(tenantId, ModuleCode.ERM)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Module ERM is not licensed for this tenant");
        }
    }

    private static void requireAdmin(User user) {
        if (!user.getRoles().contains(Role.ADMIN) && !user.getRoles().contains(Role.OWNER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private void requireUser(Long userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner user not found");
        }
    }

    private void requireRisk(Long riskId) {
        RiskRecord risk = riskRepository.findById(riskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Linked risk not found"));
        // KRI typically links ERM risks but allow any risk on ERM tenant
        if (risk.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Linked risk not found");
        }
    }

    private Kri require(long id) {
        return kriRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KRI not found"));
    }

    private Kri requireVisible(User user, long id) {
        Kri kri = require(id);
        if (RoleAccess.isContributorOnly(user) && !user.getId().equals(kri.getOwnerUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "KRI not assigned to you");
        }
        return kri;
    }

    private void audit(User user, AuditActionType action, Kri kri) {
        String role = user.getRoles().stream().map(Role::name).sorted().findFirst().orElse("UNKNOWN");
        auditTrailService.record(
                action, "KRI", user.getId(), role,
                "KRI", String.valueOf(kri.getId()), kri.getDisplayId(),
                null, null, null, null, null);
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
