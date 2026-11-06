package com.a9.arbor.grc.modules.iam.web.dto;

import java.util.List;

/** Identity for a browser session. The access token stays in an httpOnly cookie. */
public record SessionView(
        String email,
        String tenantId,
        List<String> roles,
        boolean superAdmin,
        String authType
) {
}
