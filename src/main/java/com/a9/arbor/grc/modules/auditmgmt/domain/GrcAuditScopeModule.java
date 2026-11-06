package com.a9.arbor.grc.modules.auditmgmt.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "grc_audit_scope_module")
@IdClass(GrcAuditScopeModule.Pk.class)
@Getter
@Setter
public class GrcAuditScopeModule {

    @Id
    @Column(name = "audit_id")
    private Long auditId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "module_code", length = 32)
    private EngagementScopeModule moduleCode;

    @Getter
    @Setter
    public static class Pk implements Serializable {
        private Long auditId;
        private EngagementScopeModule moduleCode;
    }
}
