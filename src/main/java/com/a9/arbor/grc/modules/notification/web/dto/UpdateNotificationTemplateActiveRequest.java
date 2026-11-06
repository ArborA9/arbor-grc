package com.a9.arbor.grc.modules.notification.web.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationTemplateActiveRequest(@NotNull Boolean active) {
}
