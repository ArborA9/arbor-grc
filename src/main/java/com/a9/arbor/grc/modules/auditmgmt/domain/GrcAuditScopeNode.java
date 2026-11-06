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
@Table(name = "grc_audit_scope_node")
@IdClass(GrcAuditScopeNode.Pk.class)
@Getter
@Setter
public class GrcAuditScopeNode {

    @Id
    @Column(name = "audit_id")
    private Long auditId;

    @Id
    @Column(name = "framework_node_id")
    private Long frameworkNodeId;

    @Getter
    @Setter
    public static class Pk implements Serializable {
        private Long auditId;
        private Long frameworkNodeId;
    }
}
