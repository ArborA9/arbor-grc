package com.a9.arbor.grc.common.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/**
 * Shared date business rules: keep in sync with {@code frontend/src/validation/dates.ts}.
 *
 * <p>Category A (action due dates): today or future when set on create/assign or when changed on update.
 */
public final class DateRules {

    private DateRules() {
    }

    /** When both dates are set, date must be on or before latest. */
    public static void requireOnOrBefore(LocalDate date, LocalDate latest, String message) {
        if (date == null || latest == null) {
            return;
        }
        if (date.isAfter(latest)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    /** Category A: optional action due date must be today or later. */
    public static void requireActionDueDateIfPresent(LocalDate date, String fieldLabel) {
        if (date == null) {
            return;
        }
        if (date.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldLabel + " cannot be in the past");
        }
    }

    /**
     * Category A on update: skip when unchanged so overdue records can still be edited.
     */
    public static void requireActionDueDateIfChanged(LocalDate current, LocalDate next, String fieldLabel) {
        if (next == null) {
            return;
        }
        if (next.equals(current)) {
            return;
        }
        requireActionDueDateIfPresent(next, fieldLabel);
    }

    /** Tolerance review date: today through +1 year (matches RiskService). */
    public static void requireToleranceReviewDate(LocalDate date) {
        if (date == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review date is required");
        }
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review date cannot be in the past");
        }
        if (date.isAfter(today.plusYears(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review date must be within 1 year");
        }
    }

    /** Category D: target on or after start when both are set. */
    public static void requireDateRange(LocalDate start, LocalDate end, String endLabel) {
        if (start == null || end == null) {
            return;
        }
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    endLabel + " must be on or after start date");
        }
    }

    /** Treatment plan dates: range + action due on target. */
    public static void validatePlanDates(LocalDate startDate, LocalDate targetDate) {
        requireDateRange(startDate, targetDate, "Target date");
        requireActionDueDateIfPresent(targetDate, "Target date");
    }
}
