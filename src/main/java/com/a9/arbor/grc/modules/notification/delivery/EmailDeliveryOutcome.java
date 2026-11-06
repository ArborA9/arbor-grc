package com.a9.arbor.grc.modules.notification.delivery;

/**
 * Result of attempting delivery for a queued outbox row.
 */
public record EmailDeliveryOutcome(String status, String errorMessage) {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";

    public static EmailDeliveryOutcome pending() {
        return new EmailDeliveryOutcome(STATUS_PENDING, null);
    }

    public static EmailDeliveryOutcome sent() {
        return new EmailDeliveryOutcome(STATUS_SENT, null);
    }

    public static EmailDeliveryOutcome failed(String message) {
        String trimmed = message == null ? "Delivery failed" : message;
        if (trimmed.length() > 500) {
            trimmed = trimmed.substring(0, 500);
        }
        return new EmailDeliveryOutcome(STATUS_FAILED, trimmed);
    }
}
