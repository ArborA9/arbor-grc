package com.a9.arbor.grc.modules.notification.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.a9.arbor.grc.modules.notification.domain.NotificationEmailOutbox;
import org.junit.jupiter.api.Test;

class SecretLinkRedactorTest {

    private static final String LINK = "https://uat.arborgrc.com/signin/reset?tenant=dev&token=MLKCzC97l3oX-yGm_1a";

    @Test
    void hidesTokenInPlainAndHtmlForPasswordLinks() {
        assertEquals("https://uat.arborgrc.com/signin/reset?tenant=dev&token=[hidden]",
                SecretLinkRedactor.redact("PASSWORD_RESET", LINK));
        String html = "<a href=\"https://x/signin/reset?tenant=dev&amp;token=abc_DEF-123\">x</a>";
        String out = SecretLinkRedactor.redact("ACCOUNT_ACTIVATION", html);
        assertTrue(out.contains("tenant=dev&amp;token=[hidden]"));
        assertFalse(out.contains("abc_DEF-123"));
    }

    @Test
    void leavesOtherTriggersAlone() {
        assertEquals(LINK, SecretLinkRedactor.redact("RISK_ASSIGNED", LINK));
    }

    @Test
    void scrubsStoredRowAfterSend() {
        NotificationEmailOutbox row = new NotificationEmailOutbox();
        row.setTriggerType("ACCOUNT_ACTIVATION");
        row.setBody("Activate: " + LINK);
        row.setBodyHtml("<p>" + LINK + "</p>");
        SecretLinkRedactor.scrubAfterSend(row);
        assertFalse(row.getBody().contains("MLKCzC97"));
        assertFalse(row.getBodyHtml().contains("MLKCzC97"));
        assertTrue(row.getBody().contains("token=[hidden]"));
    }
}
