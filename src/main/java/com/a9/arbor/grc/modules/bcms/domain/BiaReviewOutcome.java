package com.a9.arbor.grc.modules.bcms.domain;

public enum BiaReviewOutcome {
    PENDING_REVIEW,
    VERIFIED,
    RETURNED_FOR_REVISION,
    APPROVED,
    REJECTED;

    /** Specialist sign-off. Approved remains for BIAs reviewed before Verified replaced it. */
    public boolean specialistDone() {
        return this == VERIFIED || this == APPROVED;
    }

    public String display() {
        return switch (this) {
            case PENDING_REVIEW -> "Pending Review";
            case VERIFIED -> "Verified";
            case RETURNED_FOR_REVISION -> "Returned for Revision";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
        };
    }

    public static BiaReviewOutcome fromDisplay(String value) {
        if (value == null || value.isBlank()) {
            return PENDING_REVIEW;
        }
        for (BiaReviewOutcome o : values()) {
            if (o.name().equalsIgnoreCase(value) || o.display().equalsIgnoreCase(value.trim())) {
                return o;
            }
        }
        throw new IllegalArgumentException("Unknown review outcome: " + value);
    }
}
