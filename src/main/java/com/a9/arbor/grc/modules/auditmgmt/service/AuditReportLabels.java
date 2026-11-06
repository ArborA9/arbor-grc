package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditMode;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditType;
import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistAssessment;
import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistConclusion;
import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;
import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementTeamRole;
import com.a9.arbor.grc.modules.auditmgmt.domain.FindingSeverity;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistItem;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditStatus;
import com.a9.arbor.grc.modules.capa.domain.CapaStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Words shared by the audit PDF and Excel export. */
final class AuditReportLabels {

    private static final DateTimeFormatter INSTANT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private AuditReportLabels() {
    }

    static String dash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

    static String reportTitle(AuditType type) {
        if (type == null) {
            return "Audit Report";
        }
        return switch (type) {
            case INTERNAL -> "Internal Audit Report";
            case EXTERNAL -> "External Audit Report";
            case EXTERNAL_ASSESSMENT -> "External Assessment Report";
            case FOLLOW_UP -> "Follow-up Audit Report";
        };
    }

    static String typeLabel(AuditType type) {
        if (type == null) {
            return "-";
        }
        return switch (type) {
            case INTERNAL -> "Internal";
            case EXTERNAL -> "External";
            case EXTERNAL_ASSESSMENT -> "External assessment";
            case FOLLOW_UP -> "Follow-up";
        };
    }

    static String modeLabel(AuditMode mode) {
        if (mode == null) {
            return "-";
        }
        return mode == AuditMode.ASSESSMENT ? "Assessment" : "Compliance";
    }

    static String statusLabel(GrcAuditStatus status, int findingCount) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case DRAFT -> "Draft";
            case ACCESS_GRANTED -> "Access granted";
            case IN_PROGRESS -> "In progress";
            case REPORT_ISSUED -> "Report issued";
            case CANCELLED -> "Cancelled";
            case CLOSED -> findingCount == 0
                    ? "Closed with no findings"
                    : "Closed with " + findingCount + (findingCount == 1 ? " finding" : " findings");
        };
    }

    static String conclusion(ChecklistConclusion value) {
        if (value == null) {
            return "-";
        }
        return switch (value) {
            case CONFORMS -> "Conforms";
            case MAJOR_NC -> "Major NC";
            case MINOR_NC -> "Minor NC";
            case OBSERVATION -> "Observation";
            case OFI -> "OFI";
            case NOT_APPLICABLE -> "Not applicable";
            case NOT_TESTED -> "Not tested";
        };
    }

    static String severity(FindingSeverity value) {
        if (value == null) {
            return "-";
        }
        return switch (value) {
            case MAJOR_NC -> "Major NC";
            case MINOR_NC -> "Minor NC";
            case OBSERVATION -> "Observation";
            case OFI -> "OFI";
        };
    }

    static String assessment(ChecklistAssessment value) {
        if (value == null) {
            return "-";
        }
        return switch (value) {
            case YES -> "Compliant";
            case PARTIAL -> "Partial";
            case NO -> "Non-compliant";
        };
    }

    static String scopeModule(EngagementScopeModule module) {
        if (module == null) {
            return "-";
        }
        return switch (module) {
            case DASHBOARD -> "Dashboard";
            case GAP -> "Gap";
            case SOA -> "SoA";
            case RISK -> "Risk";
            case ERM -> "ERM";
            case EVIDENCE -> "Evidence";
            case BCMS -> "BCMS";
            case PRIVACY -> "Privacy";
            case ASSET -> "Assets";
        };
    }

    static String teamRole(EngagementTeamRole role) {
        return role == EngagementTeamRole.LEAD ? "Lead auditor" : "Team member";
    }

    static String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }

    static String date(LocalDate value) {
        return value == null ? "-" : value.toString();
    }

    static String instant(Instant value) {
        return value == null ? "-" : INSTANT.format(value);
    }

    static String period(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return "Dates not set";
        }
        return date(start) + " - " + date(end);
    }

    static String capaCode(Long id) {
        if (id == null) {
            return "-";
        }
        return "CAPA-" + String.format("%03d", id);
    }

    static String capaStatus(CapaStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case OPEN -> "Open";
            case IN_PROGRESS -> "In progress";
            case PENDING_VERIFICATION -> "Pending verification";
            case CLOSED -> "Closed";
            case OVERDUE -> "Overdue";
        };
    }

    static boolean countsAsReviewed(GrcAudit audit, GrcAuditChecklistItem item) {
        if (item == null || !item.isInScope()) {
            return false;
        }
        if (audit.getAuditMode() == AuditMode.ASSESSMENT) {
            return item.getAssessment() != null;
        }
        return item.getConclusion() != null && item.getConclusion() != ChecklistConclusion.NOT_TESTED;
    }
}
