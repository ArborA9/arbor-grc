package com.a9.arbor.grc.modules.auditmgmt.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "grc_audit_org_unit")
@IdClass(GrcAuditOrgUnit.Pk.class)
@Getter
@Setter
public class GrcAuditOrgUnit {

    @Id
    @Column(name = "audit_id")
    private Long auditId;

    @Id
    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Getter
    @Setter
    public static class Pk implements Serializable {
        private Long auditId;
        private Long orgUnitId;
    }
}
