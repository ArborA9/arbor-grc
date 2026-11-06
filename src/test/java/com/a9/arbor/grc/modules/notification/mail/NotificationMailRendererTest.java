package com.a9.arbor.grc.modules.notification.mail;

import com.a9.arbor.grc.modules.notification.service.NotificationCopy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationMailRendererTest {

    @Test
    void escapeKeepsMarkupOutOfHtml() {
        NotificationMailContent mail = NotificationMailRenderer.spec()
                .headline("Hello <script>alert(1)</script>")
                .lede("Vendor Acme <b>Ltd</b>")
                .cta("Open", "https://example.com/?q=\"><img")
                .render("Subject");
        assertFalse(mail.html().contains("<script>"));
        assertTrue(mail.html().contains("&lt;script&gt;"));
        assertTrue(mail.html().contains("&lt;b&gt;"));
        assertTrue(mail.html().contains("&quot;&gt;&lt;img"));
    }

    @Test
    void assignHasCtaAndNoOtpBox() {
        TprmNotificationMailComposer composer = new TprmNotificationMailComposer(null);
        NotificationMailContent mail = composer.assign(
                "Acme Cloud",
                "Acme 2026 review",
                "VQ-2026-001",
                "Core TPRM questionnaire",
                LocalDate.of(2027, 1, 15),
                Instant.parse("2027-01-20T12:00:00Z"),
                "https://portal.example/tprm?token=abc");
        assertTrue(mail.subject().startsWith("Action: Acme 2026 review"));
        assertTrue(mail.subject().contains("15 Jan 2027"));
        assertFalse(mail.subject().contains("\u2014"));
        assertTrue(mail.html().contains("Third-Party Assessment Project"));
        assertTrue(mail.html().contains("email address that received the invitation"));
        assertTrue(mail.html().contains("The passcode will be sent only to the invited email address."));
        assertTrue(mail.html().contains("Open the questionnaire"));
        assertTrue(mail.html().contains("https://portal.example/tprm?token=abc"));
        assertFalse(mail.html().toLowerCase().contains("one-time access code"));
        assertFalse(mail.plain().contains("ONE-TIME ACCESS CODE"));
        assertFalse(mail.html().contains("VENDOR_QUESTIONNAIRE_SENT"));
        assertFalse(mail.html().contains("/signin"));
    }

    @Test
    void otpShowsCodeWithoutPortalCta() {
        TprmNotificationMailComposer composer = new TprmNotificationMailComposer(null);
        NotificationMailContent mail = composer.otp(
                "Acme Cloud", "Acme 2026 review", "VQ-2026-001", "2468",
                Instant.parse("2027-01-20T12:00:00Z"));
        assertEquals("Action: Your passcode", mail.subject());
        assertTrue(mail.html().contains("Your passcode"));
        assertTrue(mail.html().contains("one-time verification code"));
        assertTrue(mail.html().contains("2468"));
        assertTrue(mail.html().toLowerCase().contains("one-time access code"));
        assertFalse(mail.html().contains("Open the questionnaire"));
        assertTrue(mail.plain().contains("ONE-TIME ACCESS CODE: 2468"));
    }

    @Test
    void closedMailIsInformationalWithoutAppLink() {
        TprmNotificationMailComposer composer = new TprmNotificationMailComposer(null);
        NotificationMailContent mail = composer.closed(
                "Acme Cloud", "Acme 2026 review", "VQ-2026-001", true, "87.5");
        assertEquals("FYI: Acme 2026 review is complete - Compliant", mail.subject());
        assertTrue(mail.html().contains("87.5%"));
        assertTrue(mail.html().contains("Compliant"));
        assertTrue(mail.html().contains("The project is complete"));
        assertTrue(mail.html().contains("Posture target"));
        assertTrue(mail.plain().contains("No action is needed from you"));
        assertFalse(mail.html().contains("Open the questionnaire"));
        assertFalse(mail.html().contains("/signin"));
        assertFalse(mail.plain().contains("/signin"));
        assertTrue(NotificationCopy.skipGenericAppLink("VENDOR_PROJECT_CLOSED", mail.plain()));
    }

    @Test
    void formatDayUsesUkMonth() {
        assertEquals("15 Jan 2027", TprmNotificationMailComposer.formatDay(LocalDate.of(2027, 1, 15)));
    }
}
