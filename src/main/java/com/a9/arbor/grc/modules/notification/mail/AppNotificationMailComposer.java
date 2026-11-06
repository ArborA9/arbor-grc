package com.a9.arbor.grc.modules.notification.mail;

import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.config.ArborNotificationProperties;
import com.a9.arbor.grc.modules.notification.service.NotificationCopy;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arbor A1-A10 HTML for every in-app notification trigger.
 * In-app bell text stays on the template. The email subject, plain body, and HTML
 * use full words and say what happened and what to do.
 */
@Service
public class AppNotificationMailComposer {

    private static final Pattern BRACKET_PREFIX = Pattern.compile("^\\[[^\\]]+]\\s*-\\s*");
    private static final Pattern URL = Pattern.compile("https?://\\S+");
    private static final String LINK_AS_WELL = "You can use the link below as well:";
    private static final String NEVER_ASK_PASSWORD =
            "For your security, Arbor GRC will never ask for your password by email or phone.";
    private static final DateTimeFormatter EMAIL_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK);

    private static final List<String[]> WORDS = List.of(
            new String[]{"CAPA", "corrective action"},
            new String[]{"ROPA", "record of processing activities"},
            new String[]{"DPIA", "data protection impact assessment"},
            new String[]{"DSR", "data subject request"},
            new String[]{"PDPL", "UAE personal data law"},
            new String[]{"BIA", "business impact analysis"},
            new String[]{"KRI", "key risk indicator"},
            new String[]{"SoA", "statement of applicability"},
            new String[]{"SOA", "statement of applicability"},
            new String[]{"ERM", "enterprise risk"},
            new String[]{"BC Head", "Business Continuity Head"}
    );

    private final TenantRepository tenantRepository;
    private final ArborNotificationProperties notificationProperties;

    public AppNotificationMailComposer(TenantRepository tenantRepository,
                                       ArborNotificationProperties notificationProperties) {
        this.tenantRepository = tenantRepository;
        this.notificationProperties = notificationProperties;
    }

    public NotificationMailContent compose(String triggerType, String renderedSubject, String renderedBody,
                                           Map<String, String> data) {
        String trigger = triggerType == null ? "" : triggerType.trim();
        Map<String, String> vars = data == null ? Map.of() : data;
        NotificationMailContent branded = branded(trigger, vars);
        if (branded != null) {
            return branded;
        }
        Kind kind = kindFor(trigger);
        String remainder = expandWords(stripBracket(renderedSubject));
        String body = expandWords(renderedBody == null ? "" : renderedBody.trim());
        String headline = remainder.isBlank() ? "You have an update in Arbor GRC" : cap(remainder);
        String lede = lede(kind, body);
        String subject = subject(kind, remainder);
        String otp = first(vars, "otp", "otpCode");
        String link = firstUrl(body, vars);
        boolean external = external(trigger, link, otp);
        boolean noSignIn = "USER_DEACTIVATED".equals(trigger);

        NotificationMailRenderer.Spec spec = NotificationMailRenderer.spec()
                .workspace(workspace())
                .preheader(preheader(lede))
                .pill(kind.pill)
                .urgency(kind.urgency)
                .headline(headline)
                .lede(lede)
                .triggerCode(trigger)
                .external(external);
        for (DetailRow row : details(vars)) {
            spec.detail(row.label, row.value);
        }
        if (otp != null) {
            String until = first(vars, "expiry", "expiresAt");
            String hint = until == null
                    ? "Arbor will never ask you for this code by phone or reply email."
                    : "Valid until " + until + ". Arbor will never ask you for this code by phone or reply email.";
            spec.otp(otp, hint);
        }
        if (link != null && otp == null) {
            spec.cta(external ? "Open secure link" : kind.cta, link);
        } else if (!noSignIn && otp == null && link == null && !NotificationCopy.skipGenericAppLink(trigger, body)) {
            String signIn = signInUrl();
            if (signIn != null) {
                spec.cta(kind.cta, signIn);
            }
        }
        return spec.render(subject);
    }

    private static String subject(Kind kind, String remainder) {
        String core = remainder.isBlank() ? "Arbor GRC update" : cap(remainder);
        if (startsWithVoice(core)) {
            return core;
        }
        return kind.voice + " " + core;
    }

    private static String lede(Kind kind, String body) {
        String facts = body.isBlank() ? "" : body;
        if (alreadyInstructs(facts)) {
            return facts;
        }
        if (facts.isBlank()) {
            return kind.next;
        }
        String gap = facts.endsWith(".") ? " " : ". ";
        return facts + gap + kind.next;
    }

    private static boolean alreadyInstructs(String body) {
        String lower = body.toLowerCase(Locale.ROOT);
        return lower.contains("open ")
                || lower.contains("sign in")
                || lower.contains("enter ")
                || lower.contains("invite ")
                || lower.contains("contact ")
                || lower.contains("download");
    }

    private static List<DetailRow> details(Map<String, String> vars) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("displayId", "Reference");
        labels.put("displayCode", "Reference");
        labels.put("registerCode", "Reference");
        labels.put("title", "Title");
        labels.put("name", "Name");
        labels.put("auditName", "Audit");
        labels.put("planName", "Plan");
        labels.put("processName", "Process");
        labels.put("activityName", "Activity");
        labels.put("exerciseName", "Exercise");
        labels.put("assessmentName", "Assessment");
        labels.put("assessmentTitle", "Assessment");
        labels.put("reportTitle", "Report");
        labels.put("vendorName", "Vendor");
        labels.put("targetDate", "Target date");
        labels.put("dueDate", "Due date");
        labels.put("startDate", "Start date");
        labels.put("endDate", "End date");
        labels.put("reviewDate", "Review date");
        labels.put("contractEnd", "Contract end");
        labels.put("expiry", "Expires");
        labels.put("expiresAt", "Expires");
        labels.put("reportDeadlineAt", "Report by");
        labels.put("severity", "Severity");
        labels.put("status", "Status");
        labels.put("decision", "Decision");
        labels.put("residualRisk", "Residual risk");
        labels.put("frameworkCode", "Framework");
        labels.put("roles", "Your role");
        labels.put("email", "Email");
        labels.put("workspaceId", "Workspace");
        List<DetailRow> rows = new ArrayList<>();
        for (Map.Entry<String, String> label : labels.entrySet()) {
            if (rows.size() >= 6) {
                break;
            }
            String value = vars.get(label.getKey());
            if (value == null || value.isBlank() || "-".equals(value.trim())) {
                continue;
            }
            if (rows.stream().anyMatch(r -> r.label.equals(label.getValue()))) {
                continue;
            }
            rows.add(new DetailRow(label.getValue(), expandWords(value.trim())));
        }
        return rows;
    }

    static Kind kindFor(String trigger) {
        String t = trigger.toUpperCase(Locale.ROOT);
        if (t.contains("OTP") || t.endsWith("_LINK") || t.equals("PASSWORD_RESET") || t.equals("ACCOUNT_ACTIVATION")
                || t.equals("USER_INVITED")) {
            return Kind.SECURE;
        }
        if (t.contains("RETURN") || t.contains("REJECT") || t.contains("SENT_BACK")) {
            return Kind.RETURNED;
        }
        if (t.contains("ESCALAT")) {
            return Kind.ESCALATED;
        }
        if (t.contains("P1") || t.contains("RED_BREACH") || t.contains("72H")
                || t.equals("DSR_OVERDUE") || t.equals("SSO_NO_ROLE") || t.equals("LOCAL_AUTH_USED")
                || t.equals("VENDOR_HIGH_RISK") || t.contains("CRITICAL")) {
            return Kind.URGENT;
        }
        if (t.contains("OVERDUE") || t.contains("EXPIRED") || t.contains("NOT_RESPONDED")
                || t.contains("NOT_SUBMITTED")) {
            return Kind.OVERDUE;
        }
        if (t.contains("DUE") || t.contains("EXPIR") || t.contains("DAY_20") || t.contains("48H")) {
            return Kind.DUE;
        }
        if (t.contains("SUBMIT") || t.contains("APPROVAL") || t.contains("PENDING") || t.contains("_REVIEW")) {
            return Kind.REVIEW;
        }
        if (t.contains("CLOSED") || t.contains("APPROVED") || t.contains("COMPLETED")
                || t.contains("DEACTIVAT") || t.contains("ROLE_CHANGED") || t.contains("AUTO_CLOSED")
                || t.contains("ACKNOWLEDGED") || t.contains("RESOLVED") || t.contains("FINALIZED")
                || t.contains("UPLOADED") || t.contains("SCHEDULED") || t.contains("TOLERATED")
                || t.equals("ADMIN_FINAL_DECISION") || t.contains("JIT")) {
            return Kind.INFO;
        }
        return Kind.ASSIGNED;
    }

    static String expandWords(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String out = raw;
        for (String[] pair : WORDS) {
            out = out.replaceAll("(?i)\\b" + Pattern.quote(pair[0]) + "\\b", pair[1]);
        }
        return out.replace('\u2014', '-').replaceAll("\\s+", " ").trim();
    }

    private static String stripBracket(String subject) {
        if (subject == null) {
            return "";
        }
        return BRACKET_PREFIX.matcher(subject.trim()).replaceFirst("").trim();
    }

    private static boolean startsWithVoice(String core) {
        String lower = core.toLowerCase(Locale.ROOT);
        return lower.startsWith("action:")
                || lower.startsWith("review:")
                || lower.startsWith("returned:")
                || lower.startsWith("due soon:")
                || lower.startsWith("overdue:")
                || lower.startsWith("escalated:")
                || lower.startsWith("urgent:")
                || lower.startsWith("fyi:");
    }

    private static boolean external(String trigger, String link, String otp) {
        return otp != null || link != null && NotificationCopy.skipGenericAppLink(trigger, link);
    }

    private static String firstUrl(String body, Map<String, String> vars) {
        Matcher inBody = URL.matcher(body);
        if (inBody.find()) {
            return trimUrl(inBody.group());
        }
        for (String value : vars.values()) {
            if (value == null) {
                continue;
            }
            Matcher m = URL.matcher(value);
            if (m.find()) {
                return trimUrl(m.group());
            }
        }
        return null;
    }

    private static String trimUrl(String url) {
        String out = url;
        while (out.endsWith(".") || out.endsWith(",") || out.endsWith(")")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    private static String first(Map<String, String> vars, String... keys) {
        for (String key : keys) {
            String value = vars.get(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String preheader(String lede) {
        if (lede.length() <= 140) {
            return lede;
        }
        return lede.substring(0, 137).trim() + "...";
    }

    private static String cap(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String t = value.trim();
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private String signInUrl() {
        if (notificationProperties == null) {
            return null;
        }
        String base = notificationProperties.getAppBaseUrl();
        if (base == null || base.isBlank()) {
            return null;
        }
        String trimmed = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String url = trimmed + "/signin";
        String tenant = TenantContext.getTenantId();
        if (tenant == null || tenant.isBlank()) {
            return url;
        }
        return url + "?tenant=" + URLEncoder.encode(tenant.trim(), StandardCharsets.UTF_8);
    }

    /**
     * Arbor's marked email copy for the invite and the three BIA messages.
     * Other triggers keep the shared composer.
     */
    private NotificationMailContent branded(String trigger, Map<String, String> vars) {
        return switch (trigger) {
            case "USER_INVITED" -> inviteMail(vars);
            case "ACCOUNT_ACTIVATION" -> activationMail(vars);
            case "PASSWORD_RESET" -> passwordResetMail(vars);
            case "BIA_ASSIGNED" -> biaAssignedMail(vars);
            case "BIA_PENDING_BC_HEAD" -> biaHeadMail(vars);
            case "BIA_CLOSED" -> biaClosedMail(vars);
            case "RISK_OWNER_NAMED" -> vars.containsKey(RISK_CUSTODIAN_NAME) ? riskOwnerInfoMail(vars) : null;
            case "RISK_ASSIGNED" -> riskAssignedMail(vars);
            case "RISK_SUBMITTED" -> riskSubmittedMail(vars);
            case "RISK_RETURNED" -> riskReturnedMail(vars);
            case "RISK_PENDING_OWNER" -> riskApprovalMail(vars);
            case "RISK_REVIEW_DUE" -> riskReviewDueMail(vars);
            case "GAP_CONTROL_ASSIGNED" -> gapAssignedMail(vars);
            case "GAP_CONTROL_SUBMITTED" -> gapSubmittedMail(vars);
            case "GAP_CONTROL_SENT_BACK" -> gapReturnedMail(vars);
            case "GAP_CONTROL_FINALIZED" -> gapApprovedMail(vars);
            case "GAP_ASSESSMENT_CREATED" -> gapAssessmentCreatedMail(vars);
            case "GAP_ASSESSMENT_COMPLETED_ASSESSOR" -> gapAssessmentCompletedMail(vars);
            case "ROPA_ASSIGNED" -> privacyRopaAssignedMail(vars);
            case "ROPA_SUBMITTED" -> privacyRopaSubmittedMail(vars);
            case "ROPA_RETURNED" -> privacyRopaReturnedMail(vars);
            case "ROPA_SEND_TO_HEAD" -> privacyRopaSendToHeadMail(vars);
            case "ROPA_HEAD_SIGN_OFF" -> privacyRopaHeadSignOffMail(vars);
            case "ROPA_READY_TO_CLOSE" -> privacyRopaReadyToCloseMail(vars);
            case "DPIA_ASSIGNED" -> privacyDpiaAssignedMail(vars);
            case "DPIA_SUBMITTED" -> privacyDpiaSubmittedMail(vars);
            case "DPIA_RETURNED" -> privacyDpiaReturnedMail(vars);
            case "DPIA_HEAD_SIGN_OFF" -> privacyDpiaHeadSignOffMail(vars);
            case "DPIA_READY_TO_CLOSE" -> privacyDpiaReadyToCloseMail(vars);
            case "RISK_OWNER_REJECTED" -> "ACCEPTANCE_REJECTED".equals(first(vars, RISK_MAIL_KIND))
                    ? riskAcceptanceReturnedMail(vars) : null;
            case "RISK_PENDING_ADMIN" -> vars.containsKey(RISK_UPDATE_IDS) ? riskPendingAdminMail(vars) : null;
            case "ADMIN_FINAL_DECISION" -> switch (String.valueOf(first(vars, RISK_MAIL_KIND))) {
                case "ACCEPTANCE_ADMIN_SENT_BACK" -> riskAcceptanceReturnedMail(vars);
                case "CLOSED" -> riskClosedMail(vars);
                default -> null;
            };
            default -> null;
        };
    }

    /** Var keys the risk workflow passes so each recipient gets Arbor's wording for their role. */
    public static final String RISK_CUSTODIAN_NAME = "custodianName";
    public static final String RISK_CUSTODIAN_ID = "custodianUserId";
    public static final String RECIPIENT_ID = "recipientUserId";
    public static final String RISK_OWNER_ID = "riskOwnerUserId";
    /** RISK_PENDING_ADMIN: comma list of users who get the update instead of the Admin action mail. */
    public static final String RISK_UPDATE_IDS = "updateUserIds";
    /**
     * RISK_SUBMITTED: SUBMITTED, RESUBMITTED or ACCEPTANCE. RISK_RETURNED: SENT_BACK or ACCEPTANCE.
     * RISK_OWNER_REJECTED: ACCEPTANCE_REJECTED. ADMIN_FINAL_DECISION: ACCEPTANCE_ADMIN_SENT_BACK or CLOSED.
     */
    public static final String RISK_MAIL_KIND = "riskMailKind";

    private NotificationMailContent riskOwnerInfoMail(Map<String, String> vars) {
        NotificationMailRenderer.Spec spec = riskShell("RISK_OWNER_NAMED", "FOR YOUR INFORMATION",
                NotificationMailRenderer.Urgency.INFO,
                "Please be informed that the following risk has been assigned to " + custodian(vars)
                        + " for treatment and evidence submission, for which you are the designated Risk Owner.",
                vars, true, false, null);
        spec.afterCta("No action is required at this stage. The status will be notified when the risk is "
                + "submitted for the approval.");
        return spec.render("Risk Assigned for Treatment | For Information");
    }

    /**
     * Risk Acceptance rejected by an approver, or sent back by the Admin. Arbor templates:
     * 1 asks the Custodian to revise, 2 is the FYI, 3 is the confirmation. When the Risk Owner
     * rejects, the Owner gets 3 and the Admin 2. When a later approver rejects, the Owner gets 2
     * and the Admin 3. On an Admin send back the Owner gets 2. Template 1 names the rejecting role.
     */
    private NotificationMailContent riskAcceptanceReturnedMail(Map<String, String> vars) {
        String trigger = "ACCEPTANCE_ADMIN_SENT_BACK".equals(first(vars, RISK_MAIL_KIND))
                ? "ADMIN_FINAL_DECISION" : "RISK_OWNER_REJECTED";
        String recipient = first(vars, RECIPIENT_ID);
        boolean toCustodian = recipient != null && recipient.equals(first(vars, RISK_CUSTODIAN_ID));
        boolean toOwner = recipient != null && recipient.equals(first(vars, RISK_OWNER_ID));
        String who = custodian(vars);
        if (toCustodian) {
            String by = first(vars, "rejectorRole");
            return riskShell(trigger, "RISK ACCEPTANCE RETURNED", NotificationMailRenderer.Urgency.RETURNED,
                    "The Risk Acceptance request for the following risk has been rejected and sent back by "
                            + (by == null ? "the Risk Owner" : by) + ". Please review the comments, update the risk, "
                            + "and resubmit it for approval in the GRC platform.",
                    vars, false, false, "Revise in Arbor GRC")
                    .render("Action Required | Risk Sent Back for Revision");
        }
        boolean ownerRejected = "true".equals(first(vars, "rejectedByOwner"));
        boolean fyi = "ADMIN_FINAL_DECISION".equals(trigger) || (ownerRejected != toOwner);
        if (fyi) {
            String rejector = first(vars, "rejectorName");
            return riskShell(trigger, "FOR YOUR INFORMATION", NotificationMailRenderer.Urgency.INFO,
                    (rejector == null || "-".equals(rejector) ? "The approver" : rejector)
                            + " has rejected the Risk Acceptance Request and returned it to " + who
                            + " for revision. The risk custodian is required to update the details and provide "
                            + "adequate supporting evidence before resubmitting the request for risk acceptance.",
                    vars, false, false, null)
                    .render("Risk Acceptance Rejected | For Your Information");
        }
        NotificationMailRenderer.Spec spec = riskShell(trigger, "RETURNED", NotificationMailRenderer.Urgency.RETURNED,
                "This is to confirm that the Risk Acceptance request for the following risk has been rejected "
                        + "and returned to " + who + " for further revision and resubmission.",
                vars, false, false, "View in Arbor GRC");
        spec.note("No action is required until the revised submission is received.");
        return spec.render("Confirmation | Risk Acceptance Request Rejected and Returned");
    }

    /** All approvers approved. Admin closes it; the Custodian and the Risk Owner get an update. */
    private NotificationMailContent riskPendingAdminMail(Map<String, String> vars) {
        String recipient = first(vars, RECIPIENT_ID);
        String updates = vars.getOrDefault(RISK_UPDATE_IDS, "");
        boolean stakeholder = recipient != null && java.util.Arrays.asList(updates.split(",")).contains(recipient);
        if (stakeholder) {
            NotificationMailRenderer.Spec spec = riskShell("RISK_PENDING_ADMIN", "FOR INFORMATION",
                    NotificationMailRenderer.Urgency.INFO,
                    "The Risk Acceptance request for the following risk has been approved by all required approvers "
                            + "and has been submitted to the Administrator for final approval and risk closure.",
                    vars, false, false, "View in Arbor GRC");
            spec.note("No action is required from you at this time.");
            return spec.render("Update | Risk Acceptance Request Approved");
        }
        return riskShell("RISK_PENDING_ADMIN", "PENDING ADMIN APPROVAL", NotificationMailRenderer.Urgency.ACTION,
                "The Risk Acceptance request for the following risk has been approved by all designated approvers "
                        + "and is now pending your review.\nPlease review the approved request and proceed with the "
                        + "final approval and closure of the risk, if appropriate.",
                vars, false, false, "View in Arbor GRC")
                .render("Action Required | Approve and Close Risk Acceptance Request");
    }

    /** One closure mail for the Admin, the Risk Owner and the Risk Custodian. */
    private NotificationMailContent riskClosedMail(Map<String, String> vars) {
        NotificationMailRenderer.Spec spec = riskShell("ADMIN_FINAL_DECISION", "RISK CLOSED",
                NotificationMailRenderer.Urgency.INFO,
                "The following risk has been successfully approved and has now been formally closed.",
                vars, false, false, "View in Arbor GRC");
        String decision = first(vars, "closureDecision");
        spec.detail("Decision", decision == null ? "-" : decision);
        spec.note("This notification is provided for your information and records.");
        return spec.render("Confirmation | Risk Closed");
    }

    private NotificationMailContent riskAssignedMail(Map<String, String> vars) {
        return riskShell("RISK_ASSIGNED", "ACTION REQUIRED", NotificationMailRenderer.Urgency.ACTION,
                "The following risk has been assigned to you for treatment. Please log in to the GRC platform "
                        + "to update the risk treatment status and upload the supporting evidence before the due date.",
                vars, true, false, "Open in Arbor GRC")
                .render("Action Required | Risk Assigned for Treatment");
    }

    private NotificationMailContent riskSubmittedMail(Map<String, String> vars) {
        String kind = first(vars, RISK_MAIL_KIND);
        String who = custodian(vars);
        String lede;
        String subject;
        if ("RESUBMITTED".equals(kind)) {
            lede = who + " has resubmitted the following risk with the updated treatment status and supporting "
                    + "evidence. Please review and proceed with the appropriate action.";
            subject = "Risk Resubmitted | Review Required";
        } else if ("ACCEPTANCE".equals(kind)) {
            lede = who + " has submitted the Risk Acceptance request for the following risk. "
                    + "Please review the submission and proceed with the appropriate action.";
            subject = "Risk Acceptance Submitted | Review Required";
        } else {
            lede = who + " has submitted the risk treatment status along with supporting evidence for the "
                    + "following risk. Please review the submission and proceed with the appropriate action.";
            subject = "Risk Treatment Update Submitted | Review Required";
        }
        return riskShell("RISK_SUBMITTED", "REVIEW", NotificationMailRenderer.Urgency.ACTION, lede,
                vars, false, false, "Review in Arbor GRC")
                .render(subject);
    }

    private NotificationMailContent riskReturnedMail(Map<String, String> vars) {
        boolean acceptance = "ACCEPTANCE".equals(first(vars, RISK_MAIL_KIND));
        String recipient = first(vars, RECIPIENT_ID);
        String custodianId = first(vars, RISK_CUSTODIAN_ID);
        boolean toCustodian = recipient != null && recipient.equals(custodianId);
        String who = custodian(vars);
        NotificationMailRenderer.Urgency returned = NotificationMailRenderer.Urgency.RETURNED;
        if (acceptance && toCustodian) {
            return riskShell("RISK_RETURNED", "RISK RETURNED", returned,
                    "Following the review of the treatment status, the following risk has been returned to you "
                            + "to initiate the Risk Acceptance process. Please log in to the GRC platform to provide "
                            + "the acceptance justification and submit it for approval.",
                    vars, true, false, "Open in Arbor GRC")
                    .render("Action Required | Risk Acceptance Initiation");
        }
        if (acceptance) {
            return riskShell("RISK_RETURNED", "RISK RETURNED", returned,
                    "The following risk has been returned to " + who + " to initiate Risk Acceptance. "
                            + "You will be notified once the acceptance request is submitted.",
                    vars, true, false, "Review in Arbor GRC")
                    .render("Risk Returned to Risk Custodian | Risk Acceptance Pending");
        }
        if (toCustodian) {
            return riskShell("RISK_RETURNED", "RISK RETURNED", returned,
                    "Your submission for the following risk has been reviewed and returned, as additional "
                            + "information or supporting evidence is required. Please review the Admin's remarks, "
                            + "update the details in the GRC platform, and resubmit.",
                    vars, true, true, "Revise in Arbor GRC")
                    .render("Action Required | Additional Information Requested");
        }
        return riskShell("RISK_RETURNED", "RISK RETURNED", returned,
                "The following risk has been returned to " + who + " for additional information, as the "
                        + "treatment status or supporting evidence was found insufficient.",
                vars, true, true, null)
                .render("Risk Returned to Risk Custodian | Additional Information Requested");
    }

    private NotificationMailContent gapAssignedMail(Map<String, String> vars) {
        String framework = gapFramework(vars);
        return gapShell("GAP_CONTROL_ASSIGNED", "ACTION REQUIRED", NotificationMailRenderer.Urgency.ACTION,
                "A Gap Assessment Control for " + framework + " has been assigned for your actions.\n"
                        + "Please review the following assigned control, complete the required assessment questions, "
                        + "and upload the necessary supporting evidence.",
                vars, "Open in Arbor GRC")
                .render("Action Required | " + framework + " Gap Assessment Control | Assigned");
    }

    private NotificationMailContent gapSubmittedMail(Map<String, String> vars) {
        String framework = gapFramework(vars);
        return gapShell("GAP_CONTROL_SUBMITTED", "ACTION REQUIRED", NotificationMailRenderer.Urgency.ACTION,
                "The Contributor has completed the Gap Assessment Control and submitted the required responses "
                        + "and supporting evidence for review.\nPlease evaluate the submission and take the appropriate action.",
                vars, "Open in Arbor GRC")
                .render("Action Required | " + framework + " Gap Assessment Control | Submitted for Review");
    }

    private NotificationMailContent gapReturnedMail(Map<String, String> vars) {
        String framework = gapFramework(vars);
        return gapShell("GAP_CONTROL_SENT_BACK", "RETURNED", NotificationMailRenderer.Urgency.RETURNED,
                "The following Gap Assessment Control has been reviewed and returned for revision.\n"
                        + "Additional information and supporting evidence are required for resubmission.",
                vars, "Open in Arbor GRC")
                .render("Action Required | " + framework + " Gap Assessment Control | Returned for Revision");
    }

    private NotificationMailContent gapApprovedMail(Map<String, String> vars) {
        String framework = gapFramework(vars);
        return gapShell("GAP_CONTROL_FINALIZED", "APPROVED", NotificationMailRenderer.Urgency.INFO,
                "The following Gap Assessment Control has been reviewed and approved.\n"
                        + "Your submitted responses and supporting evidence have been accepted and no further action is required.",
                vars, "View in Arbor GRC")
                .render("Confirmation | " + framework + " Gap Assessment Control | Approved");
    }

    private NotificationMailRenderer.Spec gapShell(String trigger, String pill,
                                                   NotificationMailRenderer.Urgency urgency, String lede,
                                                   Map<String, String> vars, String ctaLabel) {
        NotificationMailRenderer.Spec spec = NotificationMailRenderer.spec()
                .workspace(workspace())
                .preheader(preheader(lede))
                .pill(pill)
                .urgency(urgency)
                .headline("")
                .lede(lede)
                .whyYouGotThis("")
                .showAccountLine(false)
                .showFooter(false)
                .showCtaUrl(false)
                .triggerCode(trigger);
        spec.detail("Control", blankTo(first(vars, "nodeCode"), "-"));
        spec.detail("Title", blankTo(first(vars, "title"), "-"));
        String due = emailDate(first(vars, "dueDate"));
        spec.detail("Due Date", due == null ? "-" : due);
        String signIn = signInUrl();
        if (signIn != null && ctaLabel != null) {
            spec.cta(ctaLabel, signIn);
        }
        return spec;
    }

    private NotificationMailContent gapAssessmentCreatedMail(Map<String, String> vars) {
        String framework = gapFramework(vars);
        String target = emailDate(first(vars, "targetDate"));
        return gapAssessmentShell("GAP_ASSESSMENT_CREATED", "ACTION REQUIRED",
                NotificationMailRenderer.Urgency.ACTION,
                "A Gap Assessment for " + framework + " has been created and you have been named as its Assessor.\n"
                        + "Your access is unchanged. Please open the assessment, review the controls, "
                        + "and assign them to Contributors where needed.",
                vars, "Target Date", target == null ? "-" : target, "Open in Arbor GRC")
                .render("Action Required | " + framework + " Gap Assessment | Created");
    }

    private NotificationMailContent gapAssessmentCompletedMail(Map<String, String> vars) {
        String framework = gapFramework(vars);
        return gapAssessmentShell("GAP_ASSESSMENT_COMPLETED_ASSESSOR", "COMPLETED",
                NotificationMailRenderer.Urgency.INFO,
                "The Gap Assessment for " + framework + " has been completed.\n"
                        + "This confirmation is for your records. No further action is required.",
                vars, "Overall Score", blankTo(first(vars, "overallScore"), "-"), "View in Arbor GRC")
                .render("Confirmation | " + framework + " Gap Assessment | Completed");
    }

    private NotificationMailRenderer.Spec gapAssessmentShell(String trigger, String pill,
                                                             NotificationMailRenderer.Urgency urgency, String lede,
                                                             Map<String, String> vars, String extraLabel,
                                                             String extraValue, String ctaLabel) {
        NotificationMailRenderer.Spec spec = NotificationMailRenderer.spec()
                .workspace(workspace())
                .preheader(preheader(lede))
                .pill(pill)
                .urgency(urgency)
                .headline("")
                .lede(lede)
                .whyYouGotThis("")
                .showAccountLine(false)
                .showFooter(false)
                .showCtaUrl(false)
                .triggerCode(trigger);
        spec.detail("Assessment", blankTo(first(vars, "assessmentName"), "-"));
        spec.detail("Framework", gapFramework(vars));
        spec.detail(extraLabel, extraValue == null || extraValue.isBlank() ? "-" : extraValue);
        String signIn = signInUrl();
        if (signIn != null && ctaLabel != null) {
            spec.cta(ctaLabel, signIn);
        }
        return spec;
    }

    private static String gapFramework(Map<String, String> vars) {
        String name = first(vars, "frameworkName");
        if (name == null || "-".equals(name)) {
            name = first(vars, "assessmentName");
        }
        return name == null || "-".equals(name) ? "the framework" : name;
    }

    /** Privacy ROPA / DPIA: Gap-style kit (no headline, no footer, no CTA URL). */
    private NotificationMailContent privacyRopaAssignedMail(Map<String, String> vars) {
        String dept = privacyDepartment(vars);
        return privacyShell("ROPA_ASSIGNED", "ACTION REQUIRED", NotificationMailRenderer.Urgency.ACTION,
                "A Record of Processing Activities (ROPA) for " + dept
                        + " has been assigned to you as Privacy Champion.\n"
                        + "Please add the processing activities for this department and submit them for "
                        + "Data Privacy Officer review.",
                vars, "Open in Arbor GRC")
                .render("Action Required | ROPA | Assigned");
    }

    private NotificationMailContent privacyRopaSubmittedMail(Map<String, String> vars) {
        String dept = privacyDepartment(vars);
        return privacyShell("ROPA_SUBMITTED", "ACTION REQUIRED", NotificationMailRenderer.Urgency.ACTION,
                "The Privacy Champion has submitted the ROPA for " + dept + ".\n"
                        + "Please review each processing activity and complete your review in Arbor GRC.",
                vars, "Open in Arbor GRC")
                .render("Action Required | ROPA | Submitted for Review");
    }

    private NotificationMailContent privacyRopaReturnedMail(Map<String, String> vars) {
        String dept = privacyDepartment(vars);
        return privacyShell("ROPA_RETURNED", "RETURNED", NotificationMailRenderer.Urgency.RETURNED,
                "The ROPA for " + dept + " has been returned for updates.\n"
                        + "Please revise the returned processing activities and resubmit in Arbor GRC.",
                vars, "Open in Arbor GRC")
                .render("Action Required | ROPA | Returned");
    }

    private NotificationMailContent privacyRopaSendToHeadMail(Map<String, String> vars) {
        String dept = privacyDepartment(vars);
        return privacyShell("ROPA_SEND_TO_HEAD", "ACTION REQUIRED", NotificationMailRenderer.Urgency.ACTION,
                "The Data Privacy Officer has completed review of the ROPA for " + dept + ".\n"
                        + "Please open Arbor GRC and send it to the Department Head for sign-off.",
                vars, "Open in Arbor GRC")
                .render("Action Required | ROPA | Send to Department Head");
    }

    private NotificationMailContent privacyRopaHeadSignOffMail(Map<String, String> vars) {
        String dept = privacyDepartment(vars);
        return privacyShell("ROPA_HEAD_SIGN_OFF", "APPROVAL REQUIRED", NotificationMailRenderer.Urgency.ACTION,
                "The ROPA for " + dept + " is ready for your Department Head review.\n"
                        + "Please approve or return each processing activity in Arbor GRC.",
                vars, "Open in Arbor GRC")
                .render("Action Required | ROPA | Department Head Sign-off");
    }

    private NotificationMailContent privacyRopaReadyToCloseMail(Map<String, String> vars) {
        String dept = privacyDepartment(vars);
        return privacyShell("ROPA_READY_TO_CLOSE", "READY TO CLOSE", NotificationMailRenderer.Urgency.INFO,
                "The Department Head has signed off the ROPA for " + dept + ".\n"
                        + "Please open Arbor GRC to close the department ROPA.",
                vars, "Open in Arbor GRC")
                .render("Confirmation | ROPA | Ready to Close");
    }

    private NotificationMailContent privacyDpiaAssignedMail(Map<String, String> vars) {
        String dept = privacyDepartment(vars);
        return privacyShell("DPIA_ASSIGNED", "ACTION REQUIRED", NotificationMailRenderer.Urgency.ACTION,
                "A Data Protection Impact Assessment (DPIA) for " + dept
                        + " has been assigned to you as Privacy Champion.\n"
                        + "Please complete the questionnaire and submit it for Data Privacy Officer review.",
                vars, "Open in Arbor GRC")
                .render("Action Required | DPIA | Assigned");
    }

    private NotificationMailContent privacyDpiaSubmittedMail(Map<String, String> vars) {
        String dept = privacyDepartment(vars);
        return privacyShell("DPIA_SUBMITTED", "ACTION REQUIRED", NotificationMailRenderer.Urgency.ACTION,
                "The Privacy Champion has submitted the DPIA questionnaire for " + dept + ".\n"
                        + "Please review the responses in Arbor GRC.",
                vars, "Open in Arbor GRC")
                .render("Action Required | DPIA | Submitted for Review");
    }

    private NotificationMailContent privacyDpiaReturnedMail(Map<String, String> vars) {
        String dept = privacyDepartment(vars);
        return privacyShell("DPIA_RETURNED", "RETURNED", NotificationMailRenderer.Urgency.RETURNED,
                "The DPIA for " + dept + " has been returned for updates.\n"
                        + "Please revise your answers and resubmit in Arbor GRC.",
                vars, "Open in Arbor GRC")
                .render("Action Required | DPIA | Returned");
    }

    private NotificationMailContent privacyDpiaHeadSignOffMail(Map<String, String> vars) {
        String dept = privacyDepartment(vars);
        return privacyShell("DPIA_HEAD_SIGN_OFF", "APPROVAL REQUIRED", NotificationMailRenderer.Urgency.ACTION,
                "The DPIA assessment for " + dept + " is ready for your Department Head review.\n"
                        + "Please approve or return it in Arbor GRC.",
                vars, "Open in Arbor GRC")
                .render("Action Required | DPIA | Department Head Sign-off");
    }

    private NotificationMailContent privacyDpiaReadyToCloseMail(Map<String, String> vars) {
        String dept = privacyDepartment(vars);
        return privacyShell("DPIA_READY_TO_CLOSE", "READY TO CLOSE", NotificationMailRenderer.Urgency.INFO,
                "The Department Head has approved the DPIA for " + dept + ".\n"
                        + "Please open Arbor GRC to close the department DPIA.",
                vars, "Open in Arbor GRC")
                .render("Confirmation | DPIA | Ready to Close");
    }

    private NotificationMailRenderer.Spec privacyShell(String trigger, String pill,
                                                       NotificationMailRenderer.Urgency urgency, String lede,
                                                       Map<String, String> vars, String ctaLabel) {
        NotificationMailRenderer.Spec spec = NotificationMailRenderer.spec()
                .workspace(workspace())
                .preheader(preheader(lede))
                .pill(pill)
                .urgency(urgency)
                .headline("")
                .lede(lede)
                .whyYouGotThis("")
                .showAccountLine(false)
                .showFooter(false)
                .showCtaUrl(false)
                .triggerCode(trigger);
        spec.detail("Department", privacyDepartment(vars));
        String project = first(vars, "project");
        if (project != null && !project.isBlank() && !"-".equals(project)) {
            spec.detail("Project", project);
        }
        String reason = first(vars, "returnReason");
        if (reason != null && !reason.isBlank() && !"-".equals(reason)) {
            spec.detail("Return notes", reason);
        }
        String signIn = signInUrl();
        if (signIn != null && ctaLabel != null) {
            spec.cta(ctaLabel, signIn);
        }
        return spec;
    }

    private static String privacyDepartment(Map<String, String> vars) {
        String dept = first(vars, "department");
        return dept == null || dept.isBlank() || "-".equals(dept) ? "the department" : dept;
    }

    private NotificationMailContent riskApprovalMail(Map<String, String> vars) {
        return riskShell("RISK_PENDING_OWNER", "REVIEW", NotificationMailRenderer.Urgency.ACTION,
                "A Risk Acceptance request has been submitted for the following risk and requires your approval. "
                        + "Please review the details and either approve or reject the request in the GRC platform.",
                vars, false, false, "Approve in Arbor GRC")
                .render("Approval Required | Risk Acceptance Request");
    }

    /** Email Template.docx: review-date approaching reminder (title + review due date only). */
    private NotificationMailContent riskReviewDueMail(Map<String, String> vars) {
        String lede = "This is a reminder that the review date for the following risk is approaching. "
                + "Please review the risk and update its status, treatment plan, and supporting information "
                + "before the review due date to ensure it remains accurate and reflects the current risk posture.";
        NotificationMailRenderer.Spec spec = NotificationMailRenderer.spec()
                .workspace(workspace())
                .preheader(preheader(lede))
                .pill("DUE SOON")
                .urgency(NotificationMailRenderer.Urgency.ACTION)
                .headline("Risk Review Due")
                .lede(lede)
                .whyYouGotThis("")
                .showAccountLine(false)
                .showFooter(false)
                .showCtaUrl(false)
                .triggerCode("RISK_REVIEW_DUE");
        String title = first(vars, "title");
        spec.detail("Risk Title", title == null ? "-" : title);
        String review = emailDate(first(vars, "reviewDate"));
        spec.detail("Review Due Date", review == null ? "-" : review);
        String signIn = signInUrl();
        if (signIn != null) {
            spec.cta("Review Risk in Arbor GRC", signIn);
        }
        return spec.render("Action Required: Risk Review Due Soon");
    }

    private NotificationMailRenderer.Spec riskShell(String trigger, String pill,
                                                    NotificationMailRenderer.Urgency urgency, String lede,
                                                    Map<String, String> vars, boolean dueRow, boolean remarksRow,
                                                    String ctaLabel) {
        NotificationMailRenderer.Spec spec = NotificationMailRenderer.spec()
                .workspace(workspace())
                .preheader(preheader(lede))
                .pill(pill)
                .urgency(urgency)
                .headline("")
                .lede(lede)
                .whyYouGotThis("")
                .showAccountLine(false)
                .showFooter(false)
                .showCtaUrl(false)
                .triggerCode(trigger);
        String title = first(vars, "title");
        spec.detail("Risk Title", title == null ? "-" : title);
        if (dueRow) {
            String due = emailDate(first(vars, "dueDate"));
            spec.detail("Due Date", due == null ? "-" : due);
        }
        if (remarksRow) {
            String remarks = first(vars, "reason");
            spec.detail("Admin Remarks", remarks == null ? "-" : remarks);
        }
        if (ctaLabel != null) {
            String signIn = signInUrl();
            if (signIn != null) {
                spec.cta(ctaLabel, signIn);
            }
        }
        return spec;
    }

    private static String custodian(Map<String, String> vars) {
        String name = first(vars, RISK_CUSTODIAN_NAME);
        return name == null || "-".equals(name) ? "the Risk Custodian" : name;
    }

    private NotificationMailContent inviteMail(Map<String, String> vars) {
        String role = first(vars, "roles", "role");
        if (role == null || "-".equals(role)) {
            role = "user";
        }
        String workspaceName = workspace();
        String lede = "You have been invited to join Arbor GRC as " + article(role) + " " + role
                + " in the workspace " + workspaceName + ".";
        NotificationMailRenderer.Spec spec = NotificationMailRenderer.spec()
                .workspace(workspaceName)
                .preheader(preheader(lede))
                .pill(Kind.SECURE.pill)
                .urgency(Kind.SECURE.urgency)
                .headline("Welcome to Arbor GRC")
                .lede(lede)
                .showAccountLine(false)
                .triggerCode("USER_INVITED");
        for (DetailRow row : details(vars)) {
            spec.detail(row.label, row.value);
        }
        String signIn = signInUrl();
        if (signIn != null) {
            spec.cta(Kind.SECURE.cta, signIn);
        }
        return spec.render("Action Required: You've Been Invited to Arbor GRC");
    }

    private NotificationMailContent activationMail(Map<String, String> vars) {
        String workspaceName = workspace();
        String workspaceId = first(vars, "workspaceId");
        String validFor = blankTo(first(vars, "validFor"), "24 hours");
        String intro = "The " + workspaceName + " workspace has been successfully established, and you have been "
                + "designated as the Workspace Owner (Primary Administrator).";
        String lede = greeting(vars)
                + "\nWelcome to Arbor GRC."
                + "\n" + intro
                + "\nAs the Workspace Owner, you are responsible for configuring and managing your Arbor GRC "
                + "environment, including user access, organizational structure, assessments, and governance activities."
                + "\nTo get started, please activate your account using the secure link below and create your password.";
        NotificationMailRenderer.Spec spec = NotificationMailRenderer.spec()
                .workspace(workspaceName)
                .preheader(preheader(intro))
                .pill("ACCOUNT ACTIVATION")
                .urgency(NotificationMailRenderer.Urgency.ACTION)
                .headline("Welcome to Arbor GRC")
                .lede(lede)
                .detail("Organisation", workspaceName)
                .detail("Workspace ID", workspaceId)
                .detail("Email Address", first(vars, "email"))
                .detail("Role", "Workspace Owner")
                .detail("Activation Link Expires", validUntil(vars, validFor))
                .cta("Activate Account", first(vars, "actionLink"))
                .ctaUrlLabel(LINK_AS_WELL)
                .afterCta(afterActivation(workspaceId) + "\n" + NEVER_ASK_PASSWORD
                        + "\nIf you did not expect this email, ignore it. The link stops working on its own.")
                .showAccountLine(false)
                .triggerCode("ACCOUNT_ACTIVATION");
        return spec.render("Activate Your Arbor GRC Account for " + workspaceName);
    }

    private NotificationMailContent passwordResetMail(Map<String, String> vars) {
        String workspaceName = workspace();
        String validFor = blankTo(first(vars, "validFor"), "1 hour");
        String intro = "We received a request to reset the password for your Arbor GRC account.";
        NotificationMailRenderer.Spec spec = NotificationMailRenderer.spec()
                .workspace(workspaceName)
                .preheader(preheader(intro))
                .pill("PASSWORD RESET")
                .urgency(NotificationMailRenderer.Urgency.ACTION)
                .headline("Reset your password")
                .lede(greeting(vars) + "\n" + intro)
                .detail("Workspace ID", first(vars, "workspaceId"))
                .detail("Email", first(vars, "email"))
                .detail("Link valid until", validUntil(vars, validFor))
                .cta("Reset Password", first(vars, "actionLink"))
                .ctaUrlLabel(LINK_AS_WELL)
                .afterCta("Only the most recent reset email works. Requesting another one cancels this link.\n"
                        + "If you did not request this, you can ignore this email and your password will stay "
                        + "the same. " + NEVER_ASK_PASSWORD)
                .showAccountLine(false)
                .triggerCode("PASSWORD_RESET");
        return spec.render("Reset your Arbor GRC password");
    }

    private static String validUntil(Map<String, String> vars, String validFor) {
        String until = first(vars, "validUntil");
        return until == null ? validFor : until + " (" + validFor + ")";
    }

    private String afterActivation(String workspaceId) {
        String signIn = signInUrl();
        if (signIn == null || workspaceId == null) {
            return "Once activated, you can sign in with your email and new password.";
        }
        return "Once activated, you can sign in at " + signIn.replaceFirst("\\?.*$", "")
                + " using workspace ID " + workspaceId + ".";
    }

    /** "Tenant Owner" is the provisioning placeholder when no name was entered. */
    private static String greeting(Map<String, String> vars) {
        String name = first(vars, "fullName");
        if (name == null || name.isBlank() || name.trim().equalsIgnoreCase("Tenant Owner")) {
            return "Dear Sir or Madam,";
        }
        return "Dear " + name.trim() + ",";
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private NotificationMailContent biaAssignedMail(Map<String, String> vars) {
        String bia = biaName(vars);
        String org = orgUnit(vars);
        String lede = "The Business Impact Analysis \"" + bia
                + "\" has been assigned for the " + org + " organization unit.";
        NotificationMailRenderer.Spec spec = biaShell(
                "BIA_ASSIGNED",
                Kind.ASSIGNED,
                "Business Impact Analysis Assigned",
                lede,
                true);
        spec.detail("BIA name", bia);
        spec.detail("Organization unit", org);
        addDate(spec, "Due date", first(vars, "dueDate"));
        addDate(spec, "Review date", first(vars, "reviewDate"));
        return spec.render("Action Required: Business Impact Analysis has been assigned for "
                + org + " Organization Unit.");
    }

    private NotificationMailContent biaHeadMail(Map<String, String> vars) {
        String bia = biaName(vars);
        String org = orgUnit(vars);
        String lede = "The Business Impact Analysis \"" + bia + "\" for the " + org
                + " organizational unit has been submitted and is awaiting your review & approval. "
                + "Please review each processes added in the BIA and either approve it or return it for updates before approval.";
        NotificationMailRenderer.Spec spec = biaShell(
                "BIA_PENDING_BC_HEAD",
                Kind.REVIEW,
                "Business Impact Analysis Ready for Review & Approval",
                lede,
                true);
        spec.pill("Approval Required");
        spec.cta(null, null);
        String signIn = signInUrl();
        if (signIn != null) {
            spec.cta("Review & Approve", signIn);
        }
        spec.detail("BIA", bia);
        spec.detail("Organization unit", org);
        String submittedBy = first(vars, "submittedBy");
        if (submittedBy != null && !"-".equals(submittedBy)) {
            spec.detail("Submitted by", submittedBy);
        }
        addDate(spec, "Due date", first(vars, "dueDate"));
        return spec.render("Approval Required: " + bia + " for " + org + " Organization unit.");
    }

    private NotificationMailContent biaClosedMail(Map<String, String> vars) {
        String bia = biaName(vars);
        String org = orgUnit(vars);
        String lede = "The Business Impact Analysis \"" + bia + "\" for the " + org
                + " organizational unit has been successfully closed. "
                + "No further action is required at this time. This notification is provided for your records and awareness.";
        NotificationMailRenderer.Spec spec = biaShell(
                "BIA_CLOSED",
                Kind.INFO,
                "Business Impact Analysis Closed",
                lede,
                false);
        spec.detail("BIA name", bia);
        spec.detail("Organization unit", org);
        spec.detail("Status", "Closed");
        addDate(spec, "Closure date", first(vars, "closedDate"));
        return spec.render("Business Impact Analysis Successfully Closed: " + bia);
    }

    private NotificationMailRenderer.Spec biaShell(String trigger, Kind kind, String headline, String lede,
                                                   boolean accountLine) {
        NotificationMailRenderer.Spec spec = NotificationMailRenderer.spec()
                .workspace(workspace())
                .preheader(preheader(lede))
                .pill(kind.pill)
                .urgency(kind.urgency)
                .headline(headline)
                .lede(lede)
                .showAccountLine(accountLine)
                .triggerCode(trigger);
        String signIn = signInUrl();
        if (signIn != null) {
            spec.cta(kind.cta, signIn);
        }
        return spec;
    }

    private static void addDate(NotificationMailRenderer.Spec spec, String label, String raw) {
        String formatted = emailDate(raw);
        if (formatted != null) {
            spec.detail(label, formatted);
        }
    }

    private static String biaName(Map<String, String> vars) {
        String name = first(vars, "biaName");
        if (name != null && !"-".equals(name)) {
            return name;
        }
        String code = first(vars, "processName", "displayId");
        return code == null || "-".equals(code) ? "Business Impact Analysis" : code;
    }

    private static String orgUnit(Map<String, String> vars) {
        String name = first(vars, "orgUnitName");
        return name == null || "-".equals(name) ? "organization unit" : name;
    }

    private static String emailDate(String raw) {
        if (raw == null || raw.isBlank() || "-".equals(raw.trim())) {
            return null;
        }
        String t = raw.trim();
        if (t.length() >= 10 && t.charAt(4) == '-') {
            try {
                return LocalDate.parse(t.substring(0, 10)).format(EMAIL_DATE);
            } catch (DateTimeParseException ignored) {
                return t;
            }
        }
        return t;
    }

    private static String article(String role) {
        char c = Character.toLowerCase(role.trim().charAt(0));
        return (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u') ? "an" : "a";
    }

    private String workspace() {
        String id = TenantContext.getTenantId();
        if (id == null || id.isBlank() || tenantRepository == null) {
            return "Arbor GRC";
        }
        return tenantRepository.findByTenantId(id).map(t -> {
            String name = t.getName();
            return name == null || name.isBlank() ? id : name.trim();
        }).orElse(id);
    }

    enum Kind {
        ASSIGNED("ACTION", "Action:", NotificationMailRenderer.Urgency.ACTION,
                "Open in Arbor GRC",
                "Open Arbor GRC and complete this task.",
                "You received this because this work was assigned to you in Arbor GRC."),
        REVIEW("REVIEW", "Review:", NotificationMailRenderer.Urgency.ACTION,
                "Review in Arbor GRC",
                "Open Arbor GRC and review it. You can approve it or send it back.",
                "You received this because your review is needed in Arbor GRC."),
        RETURNED("RETURNED", "Returned:", NotificationMailRenderer.Urgency.RETURNED,
                "Revise in Arbor GRC",
                "Open Arbor GRC, read the comments, and submit your revision.",
                "You received this because an item you own was sent back for revision."),
        DUE("DUE SOON", "Due soon:", NotificationMailRenderer.Urgency.ACTION,
                "Open in Arbor GRC",
                "Open Arbor GRC and finish this before the due date.",
                "You received this because a due date you are responsible for is approaching."),
        OVERDUE("OVERDUE", "Overdue:", NotificationMailRenderer.Urgency.RETURNED,
                "Open in Arbor GRC",
                "This is past the due date. Open Arbor GRC and complete it as soon as you can.",
                "You received this because a due date you are responsible for has passed."),
        ESCALATED("ESCALATED", "Escalated:", NotificationMailRenderer.Urgency.RETURNED,
                "Open in Arbor GRC",
                "This was escalated because it was not completed in time. Open Arbor GRC and take the next step.",
                "You received this because the work was escalated to you."),
        URGENT("URGENT", "Urgent:", NotificationMailRenderer.Urgency.RETURNED,
                "Open in Arbor GRC",
                "This needs your attention now. Open Arbor GRC and review it.",
                "You received this because something in your workspace needs immediate attention."),
        SECURE("SECURE LINK", "Action:", NotificationMailRenderer.Urgency.ACTION,
                "Sign in",
                "Use the secure link in this email. Do not forward it.",
                "You received this because someone invited you, or asked you to confirm it is you."),
        INFO("FOR INFORMATION", "FYI:", NotificationMailRenderer.Urgency.INFO,
                "View in Arbor GRC",
                "No action is needed. This message is for your information.",
                "You received this so you know the outcome. No action is needed.");

        final String pill;
        final String voice;
        final NotificationMailRenderer.Urgency urgency;
        final String cta;
        final String next;
        final String why;

        Kind(String pill, String voice, NotificationMailRenderer.Urgency urgency,
             String cta, String next, String why) {
            this.pill = pill;
            this.voice = voice;
            this.urgency = urgency;
            this.cta = cta;
            this.next = next;
            this.why = why;
        }
    }

    private record DetailRow(String label, String value) {
    }
}
