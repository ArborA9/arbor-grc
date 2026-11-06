package com.a9.arbor.grc.modules.risk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Additional framework/control links for a risk (§22.2 RISK_FRAMEWORK_MAPPINGS). */
@Entity
@Table(name = "risk_framework_mapping")
@Getter
@Setter
public class RiskFrameworkMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "risk_id", nullable = false)
    private Long riskId;

    @Column(name = "framework_id", nullable = false)
    private Long frameworkId;

    @Column(name = "framework_code", length = 64)
    private String frameworkCode;

    @Column(name = "framework_node_id")
    private Long frameworkNodeId;

    @Column(name = "control_ref", length = 100)
    private String controlRef;

    @Column(name = "custom_ref", length = 512)
    private String customRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
