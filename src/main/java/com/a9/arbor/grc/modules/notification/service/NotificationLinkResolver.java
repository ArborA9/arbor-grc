package com.a9.arbor.grc.modules.notification.service;

/**
 * Maps trigger types to Dashboard nav ids for in-app deep links (F6.4).
 */
public final class NotificationLinkResolver {

    private NotificationLinkResolver() {
    }

    /** Returns a Dashboard {@code NavItem.id} or null when no sensible target exists. */
    public static String navIdFor(String triggerType) {
        if (triggerType == null || triggerType.isBlank()) {
            return null;
        }
        return switch (triggerType) {
            case "GAP_CONTROL_ASSIGNED", "GAP_CONTROL_SUBMITTED", "GAP_CONTROL_SENT_BACK",
                    "GAP_CONTROLS_RELEASED", "GAP_CONTROLS_SENT_BACK",
                    "GAP_CONTROL_FINALIZED", "GAP_ASSESSMENT_CREATED", "GAP_ASSESSMENT_COMPLETED",
                    "GAP_ASSESSMENT_COMPLETED_ASSESSOR", "GAP_NOT_RESPONDED",
                    "SOA_UPDATE_REQUIRED", "CONTROL_REVIEW_OVERDUE" -> "assessments";
            case "RISK_ASSIGNED", "RISK_OWNER_NAMED", "RISK_SUBMITTED", "RISK_RETURNED", "RISK_VALIDATION_REJECTED",
                    "RISK_PENDING_ADMIN", "RISK_PENDING_OWNER", "RISK_TOLERATED", "RISK_MITIGATING",
                    "TREATMENT_OVERDUE", "RISK_TOLERANCE_REVIEW_DUE", "RISK_TOLERANCE_RETURNED",
                    "RISK_REVIEW_DUE",
                    "RISK_NOT_RESPONDED", "RISK_OWNER_NOT_RESPONDED", "RISK_OWNER_REJECTED",
                    "ADMIN_FINAL_DECISION", "RISK_RE_OPENED", "RISK_OVERDUE_ESCALATION",
                    "RISK_AUTO_CLOSED", "RISK_MATRIX_CHANGED",
                    "KRI_VALUE_DUE", "KRI_VALUE_OVERDUE", "KRI_AMBER_BREACH", "KRI_RED_BREACH",
                    "KRI_RED_ACKNOWLEDGED" -> "risk";
            case "BIA_ASSIGNED", "BIA_NOT_SUBMITTED", "BIA_SUBMITTED", "BIA_RETURNED", "BIA_APPROVED",
                    "BIA_REVIEW_DUE", "BCP_APPROVAL_NEEDED", "BCP_APPROVED", "BCP_REVIEW_DUE",
                    "BCP_EXPIRED", "EXERCISE_DUE", "EXERCISE_OVERDUE", "NCEMA_CONTROL_OVERDUE" -> "bcms-bia";
            case "AUDIT_PLANNED", "AUDIT_START_DATE", "AUDIT_FINDING_LOGGED", "AUDIT_SUBMITTED",
                    "AUDIT_RETURNED", "AUDIT_COMPLETED", "AUDIT_OVERDUE", "AUDIT_REPORT_PENDING" -> "audit-register";
            case "CAPA_CREATED", "CAPA_ASSIGNED", "CAPA_SUBMITTED", "CAPA_CLOSED", "CAPA_REJECTED",
                    "CAPA_DUE_SOON", "CAPA_OVERDUE", "CAPA_OVERDUE_ESCALATION" -> "audit-capa";
            case "POLICY_SUBMITTED", "POLICY_APPROVED", "POLICY_ACK_REQUIRED", "POLICY_ACK_DEADLINE",
                    "POLICY_ACK_OVERDUE", "POLICY_REVIEW_DUE", "POLICY_EXPIRED" -> "policies";
            case "EVIDENCE_EXPIRING", "EVIDENCE_EXPIRED", "EVIDENCE_UPLOADED" -> "evidence";
            case "DSR_RECEIVED", "DSR_DAY_20", "DSR_DAY_28", "DSR_OVERDUE", "DPIA_REQUIRED",
                    "BREACH_RECORDED", "BREACH_48H_WARNING", "BREACH_72H_OVERDUE" -> "privacy-dsr";
            case "ROPA_ASSIGNED", "ROPA_SUBMITTED", "ROPA_RETURNED", "ROPA_SEND_TO_HEAD",
                    "ROPA_HEAD_SIGN_OFF", "ROPA_READY_TO_CLOSE" -> "privacy-ropa";
            case "DPIA_ASSIGNED", "DPIA_SUBMITTED", "DPIA_RETURNED", "DPIA_HEAD_SIGN_OFF",
                    "DPIA_READY_TO_CLOSE" -> "privacy-dpia-projects";
            case "VENDOR_QUESTIONNAIRE_SENT", "VENDOR_QUESTIONNAIRE_SUBMITTED", "VENDOR_QUESTIONNAIRE_RETURNED",
                    "VENDOR_REVIEW_COMPLETE", "VENDOR_PROJECT_CLOSED", "VENDOR_NOT_COMPLIANT", "VENDOR_CONTRACT_EXPIRY",
                    "VENDOR_REVIEW_DUE", "VENDOR_REVIEW_OVERDUE", "VENDOR_HIGH_RISK" -> "tprm-projects";
            case "INCIDENT_P1_REPORTED", "INCIDENT_P2_REPORTED", "INCIDENT_P3_REPORTED",
                    "INCIDENT_P4_REPORTED", "INCIDENT_ASSIGNED", "INCIDENT_OVERDUE",
                    "INCIDENT_RESOLVED", "INCIDENT_CLOSED" -> "incidents";
            case "REPORT_SCHEDULED" -> "reports";
            case "USER_INVITED", "SSO_JIT_PROVISIONED", "LOCAL_AUTH_USED", "SSO_NO_ROLE",
                 "ROLE_CHANGED", "USER_DEACTIVATED" -> "users";
            case "LICENSE_EXPIRY_REMINDER", "LICENSE_EXPIRED" -> "overview";
            default -> null;
        };
    }
}
