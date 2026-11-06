package com.a9.arbor.grc.modules.iam.web.dto;

import com.a9.arbor.grc.modules.iam.domain.SsoGroupRoleMapping;

import java.time.Instant;

public record SsoGroupRoleMappingResponse(
        Long id,
        String idpGroupName,
        String role,
        Instant createdAt
) {
    public static SsoGroupRoleMappingResponse from(SsoGroupRoleMapping m) {
        return new SsoGroupRoleMappingResponse(m.getId(), m.getIdpGroupName(), m.getRole().name(), m.getCreatedAt());
    }
}
