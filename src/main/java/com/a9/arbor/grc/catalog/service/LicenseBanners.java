package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.domain.IncludedModules;
import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.web.dto.LicenseBannerResponse;
import com.a9.arbor.grc.catalog.web.dto.ModuleLicenseResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/** Builds the tenant licence banner from current module expiry state. */
public final class LicenseBanners {

    private LicenseBanners() {
    }

    public static LicenseBannerResponse from(List<ModuleLicenseResponse> modules, boolean dismissed) {
        if (modules == null || modules.isEmpty()) {
            return null;
        }
        boolean includedLocked = modules.stream()
                .anyMatch(m -> m.enabled() && m.expired() && isIncluded(m.code()));
        List<ModuleLicenseResponse> licensed = modules.stream()
                .filter(ModuleLicenseResponse::enabled)
                .filter(m -> !isIncluded(m.code()))
                .toList();
        List<ModuleLicenseResponse> expired = licensed.stream()
                .filter(ModuleLicenseResponse::expired)
                .sorted(Comparator.comparing(ModuleLicenseResponse::code))
                .toList();
        List<ModuleLicenseResponse> expiring = licensed.stream()
                .filter(m -> !m.expired() && m.daysRemaining() != null && m.daysRemaining() >= 0 && m.daysRemaining() <= 90)
                .sorted(Comparator.comparing(ModuleLicenseResponse::daysRemaining)
                        .thenComparing(ModuleLicenseResponse::code))
                .toList();
        if (expired.isEmpty() && expiring.isEmpty()) {
            return null;
        }
        String key = bannerKey(expired, expiring);
        if (!expired.isEmpty()) {
            String names = names(expired);
            String date = formatDate(earliestExpiry(expired));
            String title = expired.size() == 1
                    ? names + " licence expired"
                    : expired.size() + " module licences expired";
            String message = includedLocked
                    ? names + " " + (expired.size() == 1 ? "is" : "are") + " view-only"
                            + (date == null ? "" : " (expired " + date + ")")
                            + ". Audit is view-only too until a licence is extended. Contact your Arbor account team."
                    : LicenseExpiryCopy.bannerMessage(names, date);
            return new LicenseBannerResponse(key, "error", title, message, dismissed);
        }
        ModuleLicenseResponse soonest = expiring.get(0);
        int days = soonest.daysRemaining() == null ? 0 : soonest.daysRemaining();
        String when = whenLabel(days);
        String names = names(expiring);
        String date = formatDate(soonest.expiresOn());
        String title = expiring.size() == 1
                ? names + " licence expires " + when
                : expiring.size() + " module licences expire " + when;
        String message = names + (expiring.size() == 1 ? " expires" : " expire")
                + (date != null ? " on " + date : "")
                + ". Contact your Arbor account team to extend before those modules become view-only.";
        return new LicenseBannerResponse(key, "warning", title, message, dismissed);
    }

    public static boolean writeLocked(List<ModuleLicenseResponse> modules) {
        if (modules == null) {
            return false;
        }
        return modules.stream().anyMatch(m -> m.enabled() && m.expired());
    }

    private static boolean isIncluded(String code) {
        try {
            return IncludedModules.isIncluded(ModuleCode.valueOf(code));
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    private static String names(List<ModuleLicenseResponse> rows) {
        List<String> labels = new ArrayList<>();
        for (ModuleLicenseResponse m : rows) {
            labels.add(m.code());
        }
        if (labels.size() == 1) {
            return labels.get(0);
        }
        if (labels.size() == 2) {
            return labels.get(0) + " and " + labels.get(1);
        }
        return String.join(", ", labels.subList(0, labels.size() - 1)) + ", and " + labels.get(labels.size() - 1);
    }

    private static LocalDate earliestExpiry(List<ModuleLicenseResponse> rows) {
        return rows.stream()
                .map(ModuleLicenseResponse::expiresOn)
                .filter(d -> d != null)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    private static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.toString();
    }

    private static String whenLabel(int days) {
        if (days <= 0) {
            return "today";
        }
        if (days == 1) {
            return "tomorrow";
        }
        if (days <= 7) {
            return "in " + days + " days";
        }
        if (days <= 14) {
            return "in 2 weeks";
        }
        if (days <= 31) {
            return "in 1 month";
        }
        if (days <= 62) {
            return "in 2 months";
        }
        return "in 3 months";
    }

    private static String bannerKey(List<ModuleLicenseResponse> expired, List<ModuleLicenseResponse> expiring) {
        StringBuilder raw = new StringBuilder();
        for (ModuleLicenseResponse m : expired) {
            raw.append("E:").append(m.code()).append(':').append(m.expiresOn()).append(';');
        }
        for (ModuleLicenseResponse m : expiring) {
            raw.append("W:").append(m.code()).append(':').append(m.expiresOn()).append(':').append(m.daysRemaining()).append(';');
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(raw.toString().toLowerCase(Locale.ROOT).hashCode());
        }
    }
}
