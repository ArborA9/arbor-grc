package com.a9.arbor.grc.modules.notification.mail;

/** Multipart mail ready for the outbox (plain + HTML). */
public record NotificationMailContent(String subject, String preheader, String plain, String html) {
}
