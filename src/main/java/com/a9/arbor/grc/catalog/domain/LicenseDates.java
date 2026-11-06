package com.a9.arbor.grc.catalog.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

/** Calendar-day licence terms stored as end-of-day UTC instants. */
public final class LicenseDates {

    public static final ZoneOffset ZONE = ZoneOffset.UTC;
    public static final int DEFAULT_TERM_YEARS = 1;
    public static final int MAX_TERM_YEARS = 10;

    private LicenseDates() {
    }

    public static Instant toExpiryInstant(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atTime(LocalTime.of(23, 59, 59)).toInstant(ZONE);
    }

    public static LocalDate toExpiryDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZONE).toLocalDate();
    }

    public static boolean isExpired(Instant expiresAt, LocalDate today) {
        LocalDate expiresOn = toExpiryDate(expiresAt);
        return expiresOn != null && today.isAfter(expiresOn);
    }

    public static Integer daysRemaining(Instant expiresAt, LocalDate today) {
        LocalDate expiresOn = toExpiryDate(expiresAt);
        if (expiresOn == null || today == null) {
            return null;
        }
        return LicenseExpiryMilestone.daysRemaining(today, expiresOn);
    }

    public static LocalDate defaultExpiryOn(LocalDate today) {
        return today.plusYears(DEFAULT_TERM_YEARS);
    }

    public static void requireValidTerm(LocalDate expiresOn, LocalDate today) {
        if (expiresOn == null) {
            throw new IllegalArgumentException("Licence expiry date is required");
        }
        if (expiresOn.isAfter(today.plusYears(MAX_TERM_YEARS))) {
            throw new IllegalArgumentException(
                    "Licence expiry date cannot be more than " + MAX_TERM_YEARS + " years from today");
        }
    }
}
