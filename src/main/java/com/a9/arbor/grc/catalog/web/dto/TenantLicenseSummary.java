package com.a9.arbor.grc.catalog.web.dto;

import com.a9.arbor.grc.catalog.domain.LicenseDates;
import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.domain.TenantModule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Compact licence roll-up for Super Admin tenant list. */
public record TenantLicenseSummary(
        int licensedCount,
        int moduleCount,
        boolean writeLocked,
        /** NONE | ACTIVE | EXPIRING | EXPIRED */
        String status,
        LocalDate earliestExpiresOn,
        Integer daysRemaining,
        boolean mixedExpiry,
        List<String> licensedCodes
) {
    public static TenantLicenseSummary from(List<TenantModule> rows, LocalDate today) {
        List<TenantModule> licensed = new ArrayList<>();
        List<String> codes = new ArrayList<>();
        if (rows != null) {
            for (TenantModule row : rows) {
                if (row == null || row.getModuleCode() == null || !row.getModuleCode().delivered()) {
                    continue;
                }
                if (row.isEnabled()) {
                    licensed.add(row);
                    codes.add(row.getModuleCode().name());
                }
            }
        }
        codes.sort(String::compareTo);

        boolean writeLocked = false;
        Set<LocalDate> dates = new HashSet<>();
        boolean anyNull = false;
        LocalDate earliest = null;
        for (TenantModule row : licensed) {
            if (LicenseDates.isExpired(row.getExpiresAt(), today)) {
                writeLocked = true;
            }
            LocalDate expiresOn = LicenseDates.toExpiryDate(row.getExpiresAt());
            if (expiresOn == null) {
                anyNull = true;
            } else {
                dates.add(expiresOn);
                if (earliest == null || expiresOn.isBefore(earliest)) {
                    earliest = expiresOn;
                }
            }
        }
        boolean mixedExpiry = licensed.size() > 1 && (dates.size() > 1 || (anyNull && !dates.isEmpty()));
        Integer daysRemaining = earliest == null ? null : LicenseDates.daysRemaining(
                LicenseDates.toExpiryInstant(earliest), today);

        String status;
        if (licensed.isEmpty()) {
            status = "NONE";
        } else if (writeLocked) {
            status = "EXPIRED";
        } else if (daysRemaining != null && daysRemaining >= 0 && daysRemaining <= 90) {
            status = "EXPIRING";
        } else {
            status = "ACTIVE";
        }

        return new TenantLicenseSummary(
                licensed.size(),
                deliveredModuleCount(),
                writeLocked,
                status,
                earliest,
                daysRemaining,
                mixedExpiry,
                List.copyOf(codes));
    }

    private static int deliveredModuleCount() {
        int count = 0;
        for (ModuleCode code : ModuleCode.values()) {
            if (code.delivered()) {
                count++;
            }
        }
        return count;
    }
}
