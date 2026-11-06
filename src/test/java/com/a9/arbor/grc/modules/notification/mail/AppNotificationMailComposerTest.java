package com.a9.arbor.grc.modules.notification.mail;

import com.a9.arbor.grc.config.ArborNotificationProperties;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppNotificationMailComposerTest {

    private final AppNotificationMailComposer composer = composerWithBase();

    private static AppNotificationMailComposer composerWithBase() {
        ArborNotificationProperties props = new ArborNotificationProperties();
        props.setAppBaseUrl("https://uat.arborgrc.com");
        return new AppNotificationMailComposer(null, props);
    }

    @Test
    void capaAssignedUsesFullWordsAndHtmlShell() {
        NotificationMailContent mail = composer.compose(
                "CAPA_ASSIGNED",
                "[Action Required] - CAPA assigned: Close the firewall gap",
                "CAPA \"Close the firewall gap\" was assigned to you. Target: 30 Sep 2026.",
                Map.of("title", "Close the firewall gap", "targetDate", "30 Sep 2026"));
        assertTrue(mail.subject().startsWith("Action: Corrective action assigned:"));
        assertTrue(mail.subject().contains("Close the firewall gap"));
        assertFalse(mail.subject().contains("[Action Required]"));
        assertFalse(mail.subject().contains("\u2014"));
        assertTrue(mail.html().contains("ARBOR"));
        assertTrue(mail.html().contains("corrective action"));
        assertTrue(mail.html().contains("Open Arbor GRC and complete this task."));
        assertFalse(mail.html().contains("CAPA_ASSIGNED"));
        assertTrue(mail.plain().contains("Target date: 30 Sep 2026"));
        assertFalse(mail.subject().contains("CAPA"));
    }

    private static Map<String, String> riskVars(String recipientId, String... extra) {
        Map<String, String> vars = new java.util.HashMap<>(Map.of(
                "displayId", "ERM-2026-006",
                "title", "Vendor outage",
                "dueDate", "2026-10-08",
                "custodianUserId", "7",
                "custodianName", "Kashyap Custodian",
                "recipientUserId", recipientId));
        for (int i = 0; i + 1 < extra.length; i += 2) {
            vars.put(extra[i], extra[i + 1]);
        }
        return vars;
    }

    private static void assertRiskShell(NotificationMailContent mail) {
        String html = mail.html();
        assertTrue(html.contains("Risk Title"));
        assertTrue(html.contains("Vendor outage"));
        assertFalse(html.contains("ERM-2026-006"));
        assertFalse(html.contains("Reference"));
        assertFalse(html.contains("Workspace"));
        assertFalse(html.contains("Hosted in Microsoft Azure"));
        assertFalse(html.contains("Message ID"));
        assertFalse(html.contains("You received this"));
        assertFalse(html.contains("SENT_BACK"));
        assertFalse(html.contains("<h1"));
        assertFalse(html.contains("\u2014"));
        assertFalse(mail.plain().contains("Message ID"));
    }

    @Test
    void riskAssignedToCustodianUsesArborCopy() {
        NotificationMailContent mail = composer.compose("RISK_ASSIGNED", "Risk assigned", "x", riskVars("7"));
        assertEquals("Action Required | Risk Assigned for Treatment", mail.subject());
        assertTrue(mail.html().contains("ACTION REQUIRED"));
        assertTrue(mail.html().contains("The following risk has been assigned to you for treatment. Please log in to "
                + "the GRC platform to update the risk treatment status and upload the supporting evidence before the due date."));
        assertTrue(mail.html().contains("Due Date"));
        assertTrue(mail.html().contains("8 Oct 2026"));
        assertTrue(mail.html().contains("Open in Arbor GRC"));
        assertFalse(mail.html().contains("word-break:break-all"));
        assertRiskShell(mail);
    }

    @Test
    void riskOwnerGetsForInformationOnAssignWithoutAButton() {
        NotificationMailContent mail = composer.compose("RISK_OWNER_NAMED", "Owner", "x", riskVars("9"));
        assertEquals("Risk Assigned for Treatment | For Information", mail.subject());
        assertTrue(mail.html().contains("FOR YOUR INFORMATION"));
        assertTrue(mail.html().contains("has been assigned to Kashyap Custodian for treatment and evidence submission, "
                + "for which you are the designated Risk Owner."));
        assertTrue(mail.html().contains("No action is required at this stage."));
        assertFalse(mail.html().contains("Regards"));
        assertFalse(mail.html().contains("GRC Team"));
        assertTrue(mail.html().contains("8 Oct 2026"));
        assertFalse(mail.html().contains("in Arbor GRC</a>"));
        assertRiskShell(mail);
    }

    @Test
    void riskSubmittedAndResubmittedToAdmin() {
        NotificationMailContent first = composer.compose("RISK_SUBMITTED", "s", "b",
                riskVars("1", "riskMailKind", "SUBMITTED"));
        assertEquals("Risk Treatment Update Submitted | Review Required", first.subject());
        assertTrue(first.html().contains("Kashyap Custodian has submitted the risk treatment status along with "
                + "supporting evidence for the following risk."));
        assertTrue(first.html().contains("Review in Arbor GRC"));
        assertFalse(first.html().contains("Due Date"));
        assertRiskShell(first);

        NotificationMailContent again = composer.compose("RISK_SUBMITTED", "s", "b",
                riskVars("1", "riskMailKind", "RESUBMITTED"));
        assertEquals("Risk Resubmitted | Review Required", again.subject());
        assertTrue(again.html().contains("Kashyap Custodian has resubmitted the following risk with the updated "
                + "treatment status and supporting evidence."));
        assertRiskShell(again);
    }

    @Test
    void sendBackHasCustodianAndAdminWording() {
        NotificationMailContent custodian = composer.compose("RISK_RETURNED", "r", "b",
                riskVars("7", "riskMailKind", "SENT_BACK", "reason", "Attach the firewall export", "decision", "SENT_BACK"));
        assertEquals("Action Required | Additional Information Requested", custodian.subject());
        assertTrue(custodian.html().contains("RISK RETURNED"));
        assertTrue(custodian.html().contains("Your submission for the following risk has been reviewed and returned"));
        assertTrue(custodian.html().contains("Admin Remarks"));
        assertTrue(custodian.html().contains("Attach the firewall export"));
        assertTrue(custodian.html().contains("Revise in Arbor GRC"));
        assertRiskShell(custodian);

        NotificationMailContent admin = composer.compose("RISK_RETURNED", "r", "b",
                riskVars("1", "riskMailKind", "SENT_BACK", "reason", "Attach the firewall export"));
        assertEquals("Risk Returned to Risk Custodian | Additional Information Requested", admin.subject());
        assertTrue(admin.html().contains("The following risk has been returned to Kashyap Custodian for additional "
                + "information, as the treatment status or supporting evidence was found insufficient."));
        assertTrue(admin.html().contains("Admin Remarks"));
        assertFalse(admin.html().contains("in Arbor GRC</a>"));
        assertRiskShell(admin);
    }

    @Test
    void acceptanceStartHasCustodianAndAdminWording() {
        NotificationMailContent custodian = composer.compose("RISK_RETURNED", "r", "b",
                riskVars("7", "riskMailKind", "ACCEPTANCE", "reason", "Acceptance required"));
        assertEquals("Action Required | Risk Acceptance Initiation", custodian.subject());
        assertTrue(custodian.html().contains("returned to you to initiate the Risk Acceptance process"));
        assertTrue(custodian.html().contains("Open in Arbor GRC"));
        assertFalse(custodian.html().contains("Admin Remarks"));
        assertRiskShell(custodian);

        NotificationMailContent admin = composer.compose("RISK_RETURNED", "r", "b",
                riskVars("1", "riskMailKind", "ACCEPTANCE"));
        assertEquals("Risk Returned to Risk Custodian | Risk Acceptance Pending", admin.subject());
        assertTrue(admin.html().contains("The following risk has been returned to Kashyap Custodian to initiate "
                + "Risk Acceptance. You will be notified once the acceptance request is submitted."));
        assertTrue(admin.html().contains("Review in Arbor GRC"));
        assertRiskShell(admin);
    }

    @Test
    void acceptanceApprovalRequestUsesOneWordingForEveryApprover() {
        NotificationMailContent mail = composer.compose("RISK_PENDING_OWNER", "p", "b",
                riskVars("9", "role", "ERM Head"));
        assertEquals("Approval Required | Risk Acceptance Request", mail.subject());
        assertTrue(mail.html().contains("A Risk Acceptance request has been submitted for the following risk and "
                + "requires your approval."));
        assertTrue(mail.html().contains("Approve in Arbor GRC"));
        assertFalse(mail.html().contains("Due Date"));
        assertRiskShell(mail);
    }

    private static final String T1_SUBJECT = "Action Required | Risk Sent Back for Revision";
    private static final String T2_SUBJECT = "Risk Acceptance Rejected | For Your Information";
    private static final String T3_SUBJECT = "Confirmation | Risk Acceptance Request Rejected and Returned";

    private NotificationMailContent rejected(String trigger, String kind, String recipient, boolean byOwner) {
        return composer.compose(trigger, "r", "b", riskVars(recipient,
                "riskMailKind", kind, "riskOwnerUserId", "9", "rejectorName", "Zara Approver",
                "rejectedByOwner", String.valueOf(byOwner), "reason", "Weak evidence"));
    }

    @Test
    void ownerRejectionSendsTemplateOneTwoThree() {
        NotificationMailContent custodian = rejected("RISK_OWNER_REJECTED", "ACCEPTANCE_REJECTED", "7", true);
        assertEquals(T1_SUBJECT, custodian.subject());
        assertTrue(custodian.html().contains("RISK ACCEPTANCE RETURNED"));
        assertTrue(custodian.html().contains("The Risk Acceptance request for the following risk has been rejected and "
                + "sent back by the Risk Owner. Please review the comments, update the risk, and resubmit it for "
                + "approval in the GRC platform."));
        assertTrue(custodian.html().contains("Revise in Arbor GRC"));
        assertRiskShell(custodian);

        NotificationMailContent admin = rejected("RISK_OWNER_REJECTED", "ACCEPTANCE_REJECTED", "1", true);
        assertEquals(T2_SUBJECT, admin.subject());
        assertTrue(admin.html().contains("FOR YOUR INFORMATION"));
        assertTrue(admin.html().contains("Zara Approver has rejected the Risk Acceptance Request and returned it to "
                + "Kashyap Custodian for revision. The risk custodian is required to update the details and provide "
                + "adequate supporting evidence before resubmitting the request for risk acceptance."));
        assertFalse(admin.html().contains("in Arbor GRC</a>"));
        assertRiskShell(admin);

        NotificationMailContent owner = rejected("RISK_OWNER_REJECTED", "ACCEPTANCE_REJECTED", "9", true);
        assertEquals(T3_SUBJECT, owner.subject());
        assertTrue(owner.html().contains(">RETURNED<"));
        assertTrue(owner.html().contains("This is to confirm that the Risk Acceptance request for the following risk "
                + "has been rejected and returned to Kashyap Custodian for further revision and resubmission."));
        assertTrue(owner.html().contains("No action is required until the revised submission is received."));
        assertTrue(owner.html().indexOf("No action is required") < owner.html().indexOf("View in Arbor GRC"));
        assertRiskShell(owner);
    }

    @Test
    void templateOneNamesTheRejectingRole() {
        for (String role : new String[] {"the CISO", "the ERM Head", "the Level 3 approver", "the Administrator"}) {
            Map<String, String> vars = riskVars("7", "riskMailKind", "ACCEPTANCE_REJECTED", "riskOwnerUserId", "9",
                    "rejectorRole", role);
            NotificationMailContent mail = composer.compose("RISK_OWNER_REJECTED", "r", "b", vars);
            assertEquals(T1_SUBJECT, mail.subject());
            assertTrue(mail.html().contains("has been rejected and sent back by " + role + ". Please review"), role);
        }
    }

    @Test
    void laterApproverRejectionSwapsOwnerAndAdminTemplates() {
        assertEquals(T1_SUBJECT, rejected("RISK_OWNER_REJECTED", "ACCEPTANCE_REJECTED", "7", false).subject());
        assertEquals(T2_SUBJECT, rejected("RISK_OWNER_REJECTED", "ACCEPTANCE_REJECTED", "9", false).subject());
        assertEquals(T3_SUBJECT, rejected("RISK_OWNER_REJECTED", "ACCEPTANCE_REJECTED", "1", false).subject());
    }

    @Test
    void adminSendBackUsesTemplateOneAndTwo() {
        assertEquals(T1_SUBJECT,
                rejected("ADMIN_FINAL_DECISION", "ACCEPTANCE_ADMIN_SENT_BACK", "7", false).subject());
        NotificationMailContent owner = rejected("ADMIN_FINAL_DECISION", "ACCEPTANCE_ADMIN_SENT_BACK", "9", false);
        assertEquals(T2_SUBJECT, owner.subject());
        assertTrue(owner.html().contains("Zara Approver has rejected"));
    }

    @Test
    void pendingAdminSplitsAdminActionFromStakeholderUpdate() {
        NotificationMailContent admin = composer.compose("RISK_PENDING_ADMIN", "p", "b",
                riskVars("1", "updateUserIds", "7,9"));
        assertEquals("Action Required | Approve and Close Risk Acceptance Request", admin.subject());
        assertTrue(admin.html().contains("PENDING ADMIN APPROVAL"));
        assertTrue(admin.html().contains("has been approved by all designated approvers and is now pending your review."));
        assertTrue(admin.html().contains("Please review the approved request and proceed with the final approval "
                + "and closure of the risk, if appropriate."));
        assertTrue(admin.html().contains("View in Arbor GRC"));
        assertRiskShell(admin);

        for (String id : new String[] {"7", "9"}) {
            NotificationMailContent update = composer.compose("RISK_PENDING_ADMIN", "p", "b",
                    riskVars(id, "updateUserIds", "7,9"));
            assertEquals("Update | Risk Acceptance Request Approved", update.subject());
            assertTrue(update.html().contains("FOR INFORMATION"));
            assertTrue(update.html().contains("has been approved by all required approvers and has been submitted to "
                    + "the Administrator for final approval and risk closure."));
            assertTrue(update.html().contains("No action is required from you at this time."));
            assertRiskShell(update);
        }
    }

    @Test
    void closureIsOneMailWithTheDecision() {
        NotificationMailContent mail = composer.compose("ADMIN_FINAL_DECISION", "c", "b",
                riskVars("1", "riskMailKind", "CLOSED", "closureDecision", "Risk Accepted", "decision", "TOLERATED"));
        assertEquals("Confirmation | Risk Closed", mail.subject());
        assertTrue(mail.html().contains("RISK CLOSED"));
        assertTrue(mail.html().contains("The following risk has been successfully approved and has now been formally closed."));
        assertTrue(mail.html().contains("Decision"));
        assertTrue(mail.html().contains("Risk Accepted"));
        assertTrue(mail.html().contains("This notification is provided for your information and records."));
        assertTrue(mail.html().contains("View in Arbor GRC"));
        assertRiskShell(mail);
    }

    @Test
    void riskReviewDueUsesEmailTemplateCopy() {
        NotificationMailContent mail = composer.compose(
                "RISK_REVIEW_DUE",
                "[Action Required] - Risk review due",
                "Risk ERM-2026-006 (Vendor outage) Next Review Date 2026-10-31 is within 30 days.",
                Map.of(
                        "displayId", "ERM-2026-006",
                        "title", "Vendor outage",
                        "reviewDate", "2026-10-31",
                        "workspaceId", "ville915"));
        assertEquals("Action Required: Risk Review Due Soon", mail.subject());
        assertTrue(mail.html().contains("DUE SOON"));
        assertTrue(mail.html().contains("Risk Review Due"));
        assertTrue(mail.html().contains("review date for the following risk is approaching"));
        assertTrue(mail.html().contains("current risk posture"));
        assertTrue(mail.html().contains("Risk Title"));
        assertTrue(mail.html().contains("Vendor outage"));
        assertTrue(mail.html().contains("Review Due Date"));
        assertTrue(mail.html().contains("31 Oct 2026"));
        assertTrue(mail.html().contains("Review Risk in Arbor GRC"));
        assertFalse(mail.html().contains("Open Arbor GRC and finish"));
        assertFalse(mail.html().contains("ERM-2026-006"));
        assertFalse(mail.html().contains("Reference"));
        assertFalse(mail.html().contains("within 30 days"));
        assertFalse(mail.html().contains("Message ID"));
        assertFalse(mail.html().contains("Hosted in Microsoft Azure"));
        assertFalse(mail.html().contains("You received this"));
    }

    @Test
    void finalDecisionWithoutAKindKeepsTheGenericMail() {
        NotificationMailContent mail = composer.compose("ADMIN_FINAL_DECISION", "Admin decision", "Decided.",
                Map.of("displayId", "ERM-2026-006", "title", "Vendor outage", "decision", "MITIGATING"));
        assertFalse(mail.html().contains("RISK CLOSED"));
        assertTrue(mail.subject().contains("Admin decision") || mail.html().contains("Decided."));
        assertFalse(mail.html().contains("Message ID"));
        assertFalse(mail.html().contains("Hosted in Microsoft Azure"));
    }

    @Test
    void ownerNamedWithoutCustodianKeepsTheGenericMail() {
        NotificationMailContent mail = composer.compose("RISK_OWNER_NAMED",
                "You are the Risk Owner: ERM-2026-006", "You are the Risk Owner for ERM-2026-006 (Vendor outage).",
                Map.of("displayId", "ERM-2026-006", "title", "Vendor outage"));
        assertFalse(mail.html().contains("designated Risk Owner"));
        assertTrue(mail.subject().contains("Risk Owner") || mail.html().contains("Vendor outage"));
        assertFalse(mail.html().contains("Message ID"));
        assertFalse(mail.html().contains("Hosted in Microsoft Azure"));
    }

    @Test
    void inviteUsesArborCopyAndFillsWorkspaceFromTheLink() {
        ArborNotificationProperties props = new ArborNotificationProperties();
        props.setAppBaseUrl("https://uat.arborgrc.com");
        AppNotificationMailComposer linked = new AppNotificationMailComposer(null, props);
        TenantContext.setTenantId("a9");
        try {
            NotificationMailContent mail = linked.compose(
                    "USER_INVITED",
                    "[Action Required] - You are invited to Arbor GRC",
                    "You have been invited to Arbor GRC as Contributor. Sign in with your email and password. Enter workspace id a9 on the sign-in page.",
                    Map.of("roles", "Contributor", "email", "kashyap@example.com", "workspaceId", "a9"));
            assertEquals("Action Required: You've Been Invited to Arbor GRC", mail.subject());
            assertTrue(mail.html().contains("Welcome to Arbor GRC"));
            assertTrue(mail.html().contains("as a Contributor in the workspace Arbor GRC"));
            assertTrue(mail.html().contains("Your role"));
            assertTrue(mail.html().contains("Workspace"));
            assertTrue(mail.html().contains("https://uat.arborgrc.com/signin?tenant=a9"));
            assertFalse(mail.html().contains("Enter workspace id"));
            assertFalse(mail.html().contains("You are receiving this email because a GRC Administrator"));
            assertFalse(mail.html().contains("Hosted in Microsoft Azure"));
            assertFalse(mail.html().contains("Message ID"));
            assertFalse(mail.html().contains("Trigger"));
            assertFalse(mail.plain().contains("Message ID"));
            assertFalse(mail.html().contains("You received this because someone invited you"));
            assertFalse(mail.html().contains("You received this because you have an Arbor GRC account."));
            assertFalse(mail.html().toLowerCase().contains("one-time access code"));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void biaAssignedKeepsDueAndReviewAndDropsWorkspace() {
        NotificationMailContent mail = composer.compose(
                "BIA_ASSIGNED",
                "[Action Required] - BIA assigned: BIA-2026-SECURI-2-3",
                "BIA \"BIA-2026-SECURI-2-3\" was assigned to you. Due 2026-11-30.",
                Map.of(
                        "processName", "BIA-2026-SECURI-2-3",
                        "biaName", "BIA 2026 Email Testing",
                        "orgUnitName", "Security",
                        "dueDate", "2026-11-30",
                        "reviewDate", "2027-10-04",
                        "workspaceId", "a9"));
        assertEquals(
                "Action Required: Business Impact Analysis has been assigned for Security Organization Unit.",
                mail.subject());
        assertTrue(mail.html().contains("Business Impact Analysis Assigned"));
        assertTrue(mail.html().contains(
                "The Business Impact Analysis &quot;BIA 2026 Email Testing&quot; has been assigned for the Security organization unit."));
        assertTrue(mail.html().contains("BIA name"));
        assertTrue(mail.html().contains("30 Nov 2026"));
        assertTrue(mail.html().contains("4 Oct 2027"));
        assertTrue(mail.html().contains("Open in Arbor GRC"));
        assertFalse(mail.html().contains("Workspace"));
        assertFalse(mail.html().contains("SECURI"));
    }

    @Test
    void biaHeadMailAsksForApprovalWithoutWorkspaceOrReviewDate() {
        NotificationMailContent mail = composer.compose(
                "BIA_PENDING_BC_HEAD",
                "[Action Required] - BIA ready for BC Head: BIA-2026-SECURI-2-3",
                "Department BIA \"BIA-2026-SECURI-2-3\" is waiting for Business Continuity Head review.",
                Map.of(
                        "biaName", "BIA 2026 Email Testing",
                        "orgUnitName", "Security",
                        "submittedBy", "Kashyap Contributor",
                        "dueDate", "2026-11-30",
                        "reviewDate", "2027-10-04",
                        "workspaceId", "a9"));
        assertEquals(
                "Approval Required: BIA 2026 Email Testing for Security Organization unit.",
                mail.subject());
        assertTrue(mail.html().contains("Approval Required"));
        assertTrue(mail.html().contains("Business Impact Analysis Ready for Review &amp; Approval"));
        assertTrue(mail.html().contains("each processes"));
        assertTrue(mail.html().contains("Submitted by"));
        assertTrue(mail.html().contains("Kashyap Contributor"));
        assertTrue(mail.html().contains("Review &amp; Approve"));
        assertTrue(mail.html().contains("30 Nov 2026"));
        assertFalse(mail.html().contains("4 Oct 2027"));
        assertFalse(mail.html().contains("Workspace"));
        assertFalse(mail.html().contains("your review is needed"));
        assertFalse(mail.html().contains("You received this because you have an Arbor GRC account."));
    }

    @Test
    void biaClosedMailUsesClosureDateAndDropsTheAccountLine() {
        NotificationMailContent mail = composer.compose(
                "BIA_CLOSED",
                "[FYI] - BIA closed: BIA-2026-SECURI-2-3",
                "Department BIA \"BIA-2026-SECURI-2-3\" was closed.",
                Map.of(
                        "biaName", "BIA 2026 Email Testing",
                        "orgUnitName", "Security",
                        "closedDate", "2026-10-06",
                        "dueDate", "2026-11-30",
                        "reviewDate", "2027-10-04",
                        "workspaceId", "a9"));
        assertEquals("Business Impact Analysis Successfully Closed: BIA 2026 Email Testing", mail.subject());
        assertTrue(mail.html().contains("Business Impact Analysis Closed"));
        assertTrue(mail.html().contains("has been successfully closed"));
        assertTrue(mail.html().contains("for your records and awareness"));
        assertTrue(mail.html().contains(">Closed<"));
        assertTrue(mail.html().contains("6 Oct 2026"));
        assertFalse(mail.html().contains("informational purposes only"));
        assertTrue(mail.html().contains("View in Arbor GRC"));
        assertFalse(mail.html().contains("30 Nov 2026"));
        assertFalse(mail.html().contains("Workspace"));
        assertFalse(mail.html().contains("so you know the outcome"));
        assertFalse(mail.html().contains("You received this because you have an Arbor GRC account."));
    }

    @Test
    void fyiDoesNotAskForActionInTheSubject() {
        NotificationMailContent mail = composer.compose(
                "CAPA_CLOSED",
                "[FYI] - CAPA closed: Firewall gap",
                "CAPA \"Firewall gap\" was closed.",
                Map.of("title", "Firewall gap"));
        assertTrue(mail.subject().startsWith("FYI:"));
        assertTrue(mail.html().contains("No action is needed"));
        assertTrue(mail.html().contains("corrective action"));
    }

    @Test
    void ownerActivationIsAWelcomeWithButtonAndNoCode() {
        String link = "https://uat.arborgrc.com/signin/reset?tenant=dev&token=abc123";
        TenantContext.setTenantId("dev");
        try {
            NotificationMailContent mail = composer.compose(
                    "ACCOUNT_ACTIVATION",
                    "Activate Your Arbor GRC Account",
                    "Your workspace dev is ready. Activate your account by setting a password: " + link,
                    Map.of("email", "owner@client.ae", "fullName", "Dani Kishore", "workspaceId", "dev",
                            "actionLink", link, "validUntil", "13 Oct 2026, 8:39 PM UAE time", "validFor", "24 hours"));
            assertTrue(mail.subject().startsWith("Activate Your Arbor GRC Account for"));
            assertFalse(mail.subject().toLowerCase().contains("reset"));
            String html = mail.html();
            assertTrue(html.contains("Welcome to Arbor GRC"));
            assertTrue(html.contains("ACCOUNT ACTIVATION"));
            assertTrue(html.contains("Activate Account"));
            assertTrue(html.contains("signin/reset?tenant=dev&amp;token=abc123") || html.contains(link));
            assertTrue(html.contains("Activation Link Expires"));
            assertTrue(html.contains("13 Oct 2026, 8:39 PM UAE time (24 hours)"));
            assertTrue(html.contains("Email Address"));
            assertTrue(html.contains("Workspace Owner"));
            assertTrue(html.contains("Dear Dani Kishore,"));
            assertTrue(html.contains("designated as the Workspace Owner (Primary Administrator)"));
            assertTrue(html.contains("user access, organizational structure, assessments, and governance activities"));
            assertTrue(html.contains("using the secure link below and create your password"));
            assertTrue(html.contains("You can use the link below as well:"));
            assertTrue(html.contains("using workspace ID dev"));
            assertTrue(html.contains("will never ask for your password by email or phone"));
            assertFalse(html.contains("Kind regards"));
            assertFalse(html.contains("contact us"));
            assertFalse(html.contains("Hosted in Microsoft Azure"));
            assertFalse(html.contains("Message ID"));
            assertFalse(mail.plain().contains("Message ID"));
            assertFalse(html.contains("You are receiving this because"));
            assertFalse(html.toLowerCase().contains("one-time"));
            assertFalse(html.toLowerCase().replace("signin/reset", "").contains("reset"));
            assertFalse(html.contains("\u2014"));
            assertTrue(mail.plain().contains(link));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void passwordResetIsLinkOnlyWithOneHourExpiry() {
        String link = "https://uat.arborgrc.com/signin/reset?tenant=dev&token=xyz";
        NotificationMailContent mail = composer.compose(
                "PASSWORD_RESET",
                "Reset your Arbor GRC password",
                "Reset your password for workspace dev: " + link,
                Map.of("email", "user@client.ae", "workspaceId", "dev", "actionLink", link,
                        "validUntil", "12 Oct 2026, 9:39 PM UAE time", "validFor", "1 hour"));
        assertEquals("Reset your Arbor GRC password", mail.subject());
        assertTrue(mail.html().contains("Dear Sir or Madam,"));
        assertTrue(mail.html().contains("Reset Password"));
        assertTrue(mail.html().contains("9:39 PM UAE time (1 hour)"));
        assertTrue(mail.html().contains("You can use the link below as well:"));
        assertTrue(mail.html().contains("Only the most recent reset email works"));
        assertTrue(mail.html().contains("your password will stay the same"));
        assertTrue(mail.html().contains("will never ask for your password by email or phone"));
        assertFalse(mail.html().contains("Kind regards"));
        assertFalse(mail.html().contains("Hosted in Microsoft Azure"));
        assertFalse(mail.html().contains("Trigger"));
        assertFalse(mail.plain().contains("Message ID"));
        assertFalse(mail.html().toLowerCase().contains("one-time"));
        assertTrue(mail.plain().contains(link));
    }

    @Test
    void gapControlMailsUseArborCopyOneControlAtATime() {
        Map<String, String> vars = Map.of(
                "frameworkName", "ISO 20000",
                "nodeCode", "4.2",
                "title", "Understanding the needs and expectations of interested parties",
                "dueDate", "2026-12-31");
        NotificationMailContent assigned = composer.compose("GAP_CONTROL_ASSIGNED", "old", "old", vars);
        assertEquals("Action Required | ISO 20000 Gap Assessment Control | Assigned", assigned.subject());
        assertTrue(assigned.html().contains("ACTION REQUIRED"));
        assertTrue(assigned.html().contains("has been assigned for your actions"));
        assertTrue(assigned.html().contains(">Control<") || assigned.html().contains("Control"));
        assertTrue(assigned.html().contains("4.2"));
        assertTrue(assigned.html().contains("Understanding the needs and expectations of interested parties"));
        assertTrue(assigned.html().contains("31 Dec 2026"));
        assertTrue(assigned.html().contains("Open in Arbor GRC"));
        assertFalse(assigned.html().contains("Workspace"));
        assertFalse(assigned.html().contains("Message ID"));
        assertFalse(assigned.html().contains("\u2014"));

        NotificationMailContent submitted = composer.compose("GAP_CONTROL_SUBMITTED", "old", "old", vars);
        assertEquals("Action Required | ISO 20000 Gap Assessment Control | Submitted for Review", submitted.subject());
        assertTrue(submitted.html().contains("submitted the required responses"));
        assertTrue(submitted.html().contains("Open in Arbor GRC"));

        NotificationMailContent returned = composer.compose("GAP_CONTROL_SENT_BACK", "old", "old", vars);
        assertEquals("Action Required | ISO 20000 Gap Assessment Control | Returned for Revision", returned.subject());
        assertTrue(returned.html().contains("RETURNED"));
        assertTrue(returned.html().contains("returned for revision"));
        assertTrue(returned.html().contains("Open in Arbor GRC"));

        NotificationMailContent approved = composer.compose("GAP_CONTROL_FINALIZED", "old", "old", vars);
        assertEquals("Confirmation | ISO 20000 Gap Assessment Control | Approved", approved.subject());
        assertTrue(approved.html().contains("APPROVED"));
        assertTrue(approved.html().contains("no further action is required"));
        assertTrue(approved.html().contains("View in Arbor GRC"));
        assertFalse(approved.html().contains("Message ID"));
    }

    @Test
    void gapAssessorMailsNameTheAssessmentWithoutChangingAccess() {
        Map<String, String> createdVars = Map.of(
                "frameworkName", "ISO 20000",
                "assessmentName", "ISO 20000 Gap 2026",
                "targetDate", "2026-12-31");
        NotificationMailContent created = composer.compose("GAP_ASSESSMENT_CREATED", "old", "old", createdVars);
        assertEquals("Action Required | ISO 20000 Gap Assessment | Created", created.subject());
        assertTrue(created.html().contains("ACTION REQUIRED"));
        assertTrue(created.html().contains("named as its Assessor"));
        assertTrue(created.html().contains("Your access is unchanged"));
        assertTrue(created.html().contains("ISO 20000 Gap 2026"));
        assertTrue(created.html().contains("31 Dec 2026"));
        assertTrue(created.html().contains("Open in Arbor GRC"));
        assertFalse(created.html().contains("Workspace"));
        assertFalse(created.html().contains("Message ID"));
        assertFalse(created.html().contains("\u2014"));

        NotificationMailContent completed = composer.compose("GAP_ASSESSMENT_COMPLETED_ASSESSOR", "old", "old",
                Map.of("frameworkName", "ISO 20000", "assessmentName", "ISO 20000 Gap 2026", "overallScore", "86%"));
        assertEquals("Confirmation | ISO 20000 Gap Assessment | Completed", completed.subject());
        assertTrue(completed.html().contains("COMPLETED"));
        assertTrue(completed.html().contains("No further action is required"));
        assertTrue(completed.html().contains("86%"));
        assertTrue(completed.html().contains("View in Arbor GRC"));
        assertFalse(completed.html().contains("Message ID"));
        assertFalse(completed.html().contains("\u2014"));
    }

    @Test
    void privacyRopaAndDpiaMailsMatchArborKit() {
        Map<String, String> vars = Map.of(
                "department", "Finance",
                "project", "ROPA 2026");
        NotificationMailContent assigned = composer.compose("ROPA_ASSIGNED", "old", "old", vars);
        assertEquals("Action Required | ROPA | Assigned", assigned.subject());
        assertTrue(assigned.html().contains("ACTION REQUIRED"));
        assertTrue(assigned.html().contains("Privacy Champion"));
        assertTrue(assigned.html().contains("Finance"));
        assertTrue(assigned.html().contains("ROPA 2026"));
        assertTrue(assigned.html().contains("Open in Arbor GRC"));
        assertFalse(assigned.html().contains("Due Date"));
        assertFalse(assigned.html().contains("Message ID"));
        assertFalse(assigned.html().contains("Hosted in Microsoft Azure"));
        assertFalse(assigned.html().contains("<h1"));
        assertFalse(assigned.html().contains("\u2014"));

        NotificationMailContent returned = composer.compose("ROPA_RETURNED", "old", "old",
                Map.of("department", "Finance", "project", "ROPA 2026",
                        "returnReason", "Missing retention period"));
        assertEquals("Action Required | ROPA | Returned", returned.subject());
        assertTrue(returned.html().contains("RETURNED"));
        assertTrue(returned.html().contains("Missing retention period"));

        NotificationMailContent ready = composer.compose("ROPA_READY_TO_CLOSE", "old", "old", vars);
        assertEquals("Confirmation | ROPA | Ready to Close", ready.subject());
        assertTrue(ready.html().contains("READY TO CLOSE"));

        NotificationMailContent dpia = composer.compose("DPIA_ASSIGNED", "old", "old",
                Map.of("department", "IT", "project", "DPIA Q1"));
        assertEquals("Action Required | DPIA | Assigned", dpia.subject());
        assertTrue(dpia.html().contains("Data Protection Impact Assessment"));
        assertTrue(dpia.html().contains("IT"));
        assertFalse(dpia.html().contains("Message ID"));
        assertFalse(dpia.html().contains("\u2014"));

        NotificationMailContent dpiaClose = composer.compose("DPIA_READY_TO_CLOSE", "old", "old",
                Map.of("department", "IT", "project", "DPIA Q1"));
        assertEquals("Confirmation | DPIA | Ready to Close", dpiaClose.subject());
    }
}
