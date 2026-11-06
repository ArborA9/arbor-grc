package com.a9.arbor.grc.catalog.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Commercial licence reminder windows. Exact calendar days from today to the expiry date.
 * Catch-up: the highest unsent window that still contains {@code daysRemaining} is sent once.
 */
public enum LicenseExpiryMilestone {
    P90(90, 61, "in 3 months"),
    P60(60, 31, "in 2 months"),
    P30(30, 15, "in 1 month"),
    P14(14, 8, "in 2 weeks"),
    P7(7, 2, "in 1 week"),
    P1(1, 1, "tomorrow"),
    EXPIRED(0, Integer.MIN_VALUE, "expired");

    private final int windowHighInclusive;
    private final int windowLowInclusive;
    private final String whenLabel;

    LicenseExpiryMilestone(int windowHighInclusive, int windowLowInclusive, String whenLabel) {
        this.windowHighInclusive = windowHighInclusive;
        this.windowLowInclusive = windowLowInclusive;
        this.whenLabel = whenLabel;
    }

    public String whenLabel() {
        return whenLabel;
    }

    public boolean isExpired() {
        return this == EXPIRED;
    }

    /** Calendar days from {@code today} to {@code expiresOn} (negative after expiry). */
    public static int daysRemaining(LocalDate today, LocalDate expiresOn) {
        if (today == null || expiresOn == null) {
            return Integer.MAX_VALUE;
        }
        return (int) ChronoUnit.DAYS.between(today, expiresOn);
    }

    public boolean contains(int daysRemaining) {
        return daysRemaining <= windowHighInclusive && daysRemaining >= windowLowInclusive;
    }

    public static Optional<LicenseExpiryMilestone> matching(int daysRemaining) {
        for (LicenseExpiryMilestone m : values()) {
            if (m.contains(daysRemaining)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }
}
