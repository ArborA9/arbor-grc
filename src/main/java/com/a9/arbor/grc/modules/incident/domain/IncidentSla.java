package com.a9.arbor.grc.modules.incident.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Response / resolution SLA helpers from §14.2. */
public final class IncidentSla {

    private IncidentSla() {
    }

    public static Instant responseDue(IncidentPriority priority, Instant from) {
        Instant base = from != null ? from : Instant.now();
        return switch (priority) {
            case P1 -> base.plus(1, ChronoUnit.HOURS);
            case P2 -> base.plus(2, ChronoUnit.HOURS);
            case P3 -> base.plus(24, ChronoUnit.HOURS);
            case P4 -> base.plus(72, ChronoUnit.HOURS);
        };
    }

    /** P1 only has an explicit resolution target (4h); others null. */
    public static Instant resolutionDue(IncidentPriority priority, Instant from) {
        if (priority != IncidentPriority.P1) {
            return null;
        }
        Instant base = from != null ? from : Instant.now();
        return base.plus(4, ChronoUnit.HOURS);
    }

    public static String reportedTrigger(IncidentPriority priority) {
        return switch (priority) {
            case P1 -> "INCIDENT_P1_REPORTED";
            case P2 -> "INCIDENT_P2_REPORTED";
            case P3 -> "INCIDENT_P3_REPORTED";
            case P4 -> "INCIDENT_P4_REPORTED";
        };
    }
}
