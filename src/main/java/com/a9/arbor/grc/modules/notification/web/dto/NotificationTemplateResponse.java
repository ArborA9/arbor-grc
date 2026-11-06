package com.a9.arbor.grc.modules.notification.web.dto;

public record NotificationTemplateResponse(
        Long id,
        String triggerType,
        String subjectTemplate,
        boolean channelEmail,
        boolean channelInApp,
        int version,
        boolean active
) {
}
