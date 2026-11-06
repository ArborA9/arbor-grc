package com.a9.arbor.grc.modules.dashboard.service;

import com.a9.arbor.grc.modules.dashboard.domain.ReportDelivery;
import com.a9.arbor.grc.modules.dashboard.domain.ReportSchedule;
import com.a9.arbor.grc.modules.dashboard.domain.ReportScheduleFrequency;
import com.a9.arbor.grc.modules.dashboard.repo.ReportDeliveryRepository;
import com.a9.arbor.grc.modules.dashboard.repo.ReportScheduleRepository;
import com.a9.arbor.grc.modules.dashboard.web.dto.CreateReportScheduleRequest;
import com.a9.arbor.grc.modules.dashboard.web.dto.ReportDeliveryResponse;
import com.a9.arbor.grc.modules.dashboard.web.dto.ReportScheduleResponse;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportScheduleService {

    private final ReportScheduleRepository scheduleRepository;
    private final ReportDeliveryRepository deliveryRepository;
    private final ReportService reportService;
    private final NotificationService notificationService;

    public ReportScheduleService(ReportScheduleRepository scheduleRepository,
                                 ReportDeliveryRepository deliveryRepository,
                                 ReportService reportService,
                                 NotificationService notificationService) {
        this.scheduleRepository = scheduleRepository;
        this.deliveryRepository = deliveryRepository;
        this.reportService = reportService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ReportScheduleResponse> listSchedules() {
        return scheduleRepository.findAll().stream()
                .map(ReportScheduleResponse::from)
                .toList();
    }

    @Transactional
    public ReportScheduleResponse create(User admin, CreateReportScheduleRequest req) {
        if (req.recipientUserIds().size() > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most 3 recipients allowed");
        }
        ReportSchedule schedule = new ReportSchedule();
        schedule.setReportId(req.reportId());
        schedule.setFormat(req.format());
        schedule.setFrequency(req.frequency());
        schedule.setRecipientUserIds(req.recipientUserIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        schedule.setSoaId(req.soaId());
        schedule.setGapId(req.gapId());
        schedule.setAuditId(req.auditId());
        schedule.setExerciseId(req.exerciseId());
        schedule.setVariant(req.variant());
        schedule.setNextRunAt(Instant.now());
        schedule.setCreatedByUserId(admin.getId());
        scheduleRepository.save(schedule);
        return ReportScheduleResponse.from(schedule);
    }

    @Transactional
    public void deactivate(User admin, long id) {
        ReportSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
        schedule.setActive(false);
        scheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public List<ReportDeliveryResponse> listDeliveries(User user) {
        return deliveryRepository.findByRecipientUserIdOrderByDeliveredAtDesc(user.getId()).stream()
                .map(ReportDeliveryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExportFile downloadDelivery(User user, long deliveryId) {
        ReportDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
        if (!delivery.getRecipientUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Delivery not available");
        }
        return new ExportFile(delivery.getFileBytes(), delivery.getFilename(), delivery.getContentType());
    }

    @Transactional
    public int processDueSchedules(Instant asOf) {
        deliveryRepository.deleteByDeliveredAtBefore(asOf.minus(365, ChronoUnit.DAYS));
        List<ReportSchedule> due = scheduleRepository.findByActiveTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(asOf);
        int processed = 0;
        for (ReportSchedule schedule : due) {
            try {
                deliverSchedule(schedule);
                schedule.setLastRunAt(asOf);
                schedule.setNextRunAt(nextRun(schedule.getFrequency(), asOf));
                scheduleRepository.save(schedule);
                processed++;
            } catch (Exception e) {
                schedule.setNextRunAt(asOf.plus(1, ChronoUnit.DAYS));
                scheduleRepository.save(schedule);
            }
        }
        return processed;
    }

    private void deliverSchedule(ReportSchedule schedule) {
        User systemUser = new User();
        systemUser.setId(schedule.getCreatedByUserId());
        systemUser.setEmail("system@arborgrc.local");
        systemUser.setFullName("Scheduled Report");
        ExportFile file = reportService.export(
                systemUser,
                schedule.getReportId(),
                schedule.getFormat(),
                schedule.getSoaId(),
                schedule.getGapId(),
                schedule.getAuditId(),
                schedule.getExerciseId(),
                schedule.getVariant());
        for (String recipientId : schedule.getRecipientUserIds().split(",")) {
            if (recipientId.isBlank()) {
                continue;
            }
            long userId = Long.parseLong(recipientId.trim());
            ReportDelivery delivery = new ReportDelivery();
            delivery.setScheduleId(schedule.getId());
            delivery.setReportId(schedule.getReportId());
            delivery.setFilename(file.filename());
            delivery.setContentType(file.contentType());
            delivery.setFileBytes(file.bytes());
            delivery.setRecipientUserId(userId);
            deliveryRepository.save(delivery);

            Map<String, String> data = new LinkedHashMap<>();
            data.put("reportTitle", schedule.getReportId());
            data.put("format", schedule.getFormat());
            notificationService.trigger("REPORT_SCHEDULED", String.valueOf(delivery.getId()),
                    List.of(String.valueOf(userId)), data);
        }
    }

    static Instant nextRun(ReportScheduleFrequency frequency, Instant from) {
        return switch (frequency) {
            case DAILY -> from.plus(1, ChronoUnit.DAYS);
            case WEEKLY -> from.plus(7, ChronoUnit.DAYS);
            case MONTHLY -> from.plus(30, ChronoUnit.DAYS);
            case QUARTERLY -> from.plus(90, ChronoUnit.DAYS);
            case ANNUALLY -> from.plus(365, ChronoUnit.DAYS);
        };
    }
}
