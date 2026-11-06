package com.a9.arbor.grc.catalog.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseExpiryEmailComposerTest {

    @Test
    void reminderMatchesArborSubscriptionCopy() {
        LicenseExpiryEmailComposer.Content mail = LicenseExpiryEmailComposer.compose(
                "Abu Dhabi Family Care Authority",
                "uatfca",
                "in 2 months",
                LocalDate.of(2026, 11, 30),
                List.of("BCMS", "ERM", "ISMS"),
                false,
                "https://uat.arborgrc.com/signin",
                "Rakesh Jangid");

        assertEquals(
                "Action Recommended | Your Arbor GRC Subscription Expires in 2 Months",
                mail.subject());
        assertTrue(mail.html().contains("LICENSE EXPIRY NOTICE"));
        assertTrue(mail.html().contains("Your Arbor GRC Subscription Expires on 30 November 2026"));
        assertTrue(mail.html().contains("Dear Rakesh Jangid,"));
        assertTrue(mail.html().contains("friendly reminder"));
        assertTrue(mail.html().contains("Arbor account representative"));
        assertTrue(mail.html().contains("Organisation"));
        assertTrue(mail.html().contains("Workspace ID"));
        assertTrue(mail.html().contains("uatfca"));
        assertTrue(mail.html().contains("Licensed Modules"));
        assertTrue(mail.html().contains("BCMS, ERM, and ISMS"));
        assertTrue(mail.html().contains("Expiry Date"));
        assertTrue(mail.html().contains("Current Status"));
        assertTrue(mail.html().contains("Active"));
        assertTrue(mail.html().contains("Sign in to Arbor GRC"));
        assertTrue(mail.html().contains("https://uat.arborgrc.com/signin"));
        assertFalse(mail.html().contains("view-only"));
        assertFalse(mail.html().contains("\u2014"));
        assertTrue(mail.plain().contains("Dear Rakesh Jangid,"));
        assertTrue(mail.plain().contains("Current Status: Active"));
    }

    @Test
    void expiredUsesActionRequiredAndExpiredStatus() {
        LicenseExpiryEmailComposer.Content mail = LicenseExpiryEmailComposer.compose(
                "Villa 515 Org",
                "villa515uat",
                "expired",
                LocalDate.of(2026, 9, 1),
                List.of("ERM"),
                true,
                "https://uat.arborgrc.com/signin",
                null);

        assertEquals("Action Required | Your Arbor GRC Subscription Has Expired", mail.subject());
        assertTrue(mail.html().contains("Your Arbor GRC Subscription Has Expired"));
        assertTrue(mail.html().contains("Dear Sir or Madam,"));
        assertTrue(mail.html().contains("Expired"));
        assertTrue(mail.html().contains("restore"));
    }

    @Test
    void subjectWhenTitleCasesUnit() {
        assertEquals("in 2 Months", LicenseExpiryEmailComposer.subjectWhen("in 2 months"));
        assertEquals("in 1 Week", LicenseExpiryEmailComposer.subjectWhen("in 1 week"));
        assertEquals("Tomorrow", LicenseExpiryEmailComposer.subjectWhen("tomorrow"));
    }
}
