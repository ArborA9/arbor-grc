package com.a9.arbor.grc.modules.notification.delivery;

import com.a9.arbor.grc.modules.notification.domain.NotificationEmailOutbox;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Activation and reset emails carry a live set-password token. Owners and Admins can read the
 * email outbox, so the token is never shown there and is dropped from the stored row once sent.
 */
public final class SecretLinkRedactor {

    public static final String HIDDEN = "[hidden]";

    private static final Set<String> SECRET_LINK_TRIGGERS = Set.of("ACCOUNT_ACTIVATION", "PASSWORD_RESET");
    private static final Pattern TOKEN = Pattern.compile("(token=)[A-Za-z0-9_\\-]+");

    private SecretLinkRedactor() {
    }

    public static boolean appliesTo(String triggerType) {
        return triggerType != null && SECRET_LINK_TRIGGERS.contains(triggerType);
    }

    public static String redact(String triggerType, String text) {
        if (text == null || !appliesTo(triggerType)) {
            return text;
        }
        return TOKEN.matcher(text).replaceAll("$1" + HIDDEN);
    }

    /** Call only after the message has left: delivery reads the stored body. */
    public static void scrubAfterSend(NotificationEmailOutbox row) {
        if (!appliesTo(row.getTriggerType())) {
            return;
        }
        row.setBody(redact(row.getTriggerType(), row.getBody()));
        row.setBodyHtml(redact(row.getTriggerType(), row.getBodyHtml()));
    }
}
