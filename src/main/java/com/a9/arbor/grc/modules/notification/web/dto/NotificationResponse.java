package com.a9.arbor.grc.modules.notification.web.dto;

import com.a9.arbor.grc.modules.notification.domain.Notification;

import java.time.Instant;

public record NotificationResponse(
        long id,
        String triggerType,
        String title,
        String body,
        String recordType,
        String recordId,
        String link,
        boolean unread,
        Instant readAt,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getTriggerType(),
                n.getTitle(),
                n.getBody(),
                n.getRecordType(),
                n.getRecordId(),
                n.getLink(),
                n.isUnread(),
                n.getReadAt(),
                n.getCreatedAt());
    }
}
