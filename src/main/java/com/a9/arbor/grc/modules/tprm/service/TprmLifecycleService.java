package com.a9.arbor.grc.modules.tprm.service;

import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.tprm.domain.TprmCriticality;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendor;
import com.a9.arbor.grc.modules.tprm.domain.VendorStatus;
import com.a9.arbor.grc.modules.tprm.repo.TprmVendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TprmLifecycleService {

    public record LifecycleRunResult(int reviewDue, int reviewOverdue, int contractExpiry) {
    }

    private final TprmVendorRepository vendorRepository;
    private final NotificationService notificationService;

    public TprmLifecycleService(TprmVendorRepository vendorRepository,
                                NotificationService notificationService) {
        this.vendorRepository = vendorRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public LifecycleRunResult processCurrentTenant(LocalDate asOf) {
        int reviewDue = 0;
        int reviewOverdue = 0;
        int contractExpiry = 0;
        for (TprmVendor vendor : vendorRepository.findByStatus(VendorStatus.ACTIVE)) {
            if (vendor.getNextReviewDate() != null) {
                long daysUntil = ChronoUnit.DAYS.between(asOf, vendor.getNextReviewDate());
                int warnDays = warnDaysBefore(vendor.getCriticality());
                if (daysUntil == warnDays && vendor.getReviewDueNotifiedAt() == null) {
                    notificationService.trigger("VENDOR_REVIEW_DUE", String.valueOf(vendor.getId()),
                            List.of("ADMIN", "OWNER"), vendorData(vendor));
                    vendor.setReviewDueNotifiedAt(Instant.now());
                    vendorRepository.save(vendor);
                    reviewDue++;
                }
                if (daysUntil < 0 && vendor.getReviewOverdueNotifiedAt() == null) {
                    notificationService.trigger("VENDOR_REVIEW_OVERDUE", String.valueOf(vendor.getId()),
                            List.of("ADMIN", "OWNER"), vendorData(vendor));
                    vendor.setReviewOverdueNotifiedAt(Instant.now());
                    vendorRepository.save(vendor);
                    reviewOverdue++;
                }
            }
            if (vendor.getContractEnd() != null) {
                long daysUntilContract = ChronoUnit.DAYS.between(asOf, vendor.getContractEnd());
                if (daysUntilContract <= 30 && daysUntilContract >= 0 && vendor.getContractExpiryNotifiedAt() == null) {
                    notificationService.trigger("VENDOR_CONTRACT_EXPIRY", String.valueOf(vendor.getId()),
                            List.of("ADMIN", "OWNER"), vendorData(vendor));
                    vendor.setContractExpiryNotifiedAt(Instant.now());
                    vendorRepository.save(vendor);
                    contractExpiry++;
                }
            }
        }
        return new LifecycleRunResult(reviewDue, reviewOverdue, contractExpiry);
    }

    private static int warnDaysBefore(TprmCriticality criticality) {
        if (criticality == TprmCriticality.CRITICAL) {
            return 90;
        }
        return 30;
    }

    private static Map<String, String> vendorData(TprmVendor vendor) {
        Map<String, String> data = new HashMap<>();
        data.put("vendorName", vendor.getName());
        data.put("reviewDate", vendor.getNextReviewDate() != null ? vendor.getNextReviewDate().toString() : "-");
        data.put("criticality", vendor.getCriticality() != null ? vendor.getCriticality().name() : "-");
        data.put("tier", vendor.getTier() != null ? vendor.getTier() : "-");
        data.put("contractCode", vendor.getContractCode() != null ? vendor.getContractCode() : "-");
        data.put("contractEnd", vendor.getContractEnd() != null ? vendor.getContractEnd().toString() : "-");
        return data;
    }
}
