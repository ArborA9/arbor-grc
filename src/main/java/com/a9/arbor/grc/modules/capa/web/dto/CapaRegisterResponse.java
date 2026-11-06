package com.a9.arbor.grc.modules.capa.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.capa.domain.GrcCapaRegister;

import java.time.Instant;

public record CapaRegisterResponse(
        Long id,
        Integer year,
        String name,
        Long auditId,
        String auditName,
        String auditDisplayId,
        String conductedBy,
        String description,
        int capaCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static CapaRegisterResponse from(GrcCapaRegister r, GrcAudit audit, int capaCount) {
        return new CapaRegisterResponse(
                r.getId(),
                r.getRegisterYear(),
                r.getName(),
                r.getAuditId(),
                audit != null ? audit.getName() : null,
                audit != null ? audit.getDisplayId() : null,
                r.getConductedBy(),
                r.getDescription(),
                capaCount,
                r.getCreatedAt(),
                r.getUpdatedAt());
    }
}
