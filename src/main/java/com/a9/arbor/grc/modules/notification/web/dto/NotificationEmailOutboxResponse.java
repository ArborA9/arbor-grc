package com.a9.arbor.grc.modules.notification.web.dto;

import com.a9.arbor.grc.modules.notification.domain.NotificationEmailOutbox;
import com.a9.arbor.grc.modules.notification.delivery.EmailDeliveryService;
import com.a9.arbor.grc.modules.notification.delivery.SecretLinkRedactor;

import java.time.Instant;

public record NotificationEmailOutboxResponse(
        Long id,
        String triggerType,
        String toEmail,
        String subject,
        String body,
        String bodyHtml,
        String status,
        Instant createdAt,
        Instant sentAt,
        String errorMessage,
        String deliveryNote
) {
    public static NotificationEmailOutboxResponse from(NotificationEmailOutbox row, EmailDeliveryService deliveryService) {
        return new NotificationEmailOutboxResponse(
                row.getId(),
                row.getTriggerType(),
                row.getToEmail(),
                row.getSubject(),
                SecretLinkRedactor.redact(row.getTriggerType(), row.getBody()),
                SecretLinkRedactor.redact(row.getTriggerType(), row.getBodyHtml()),
                row.getStatus(),
                row.getCreatedAt(),
                row.getSentAt(),
                row.getErrorMessage(),
                deliveryService.deliveryNote(row));
    }
}
