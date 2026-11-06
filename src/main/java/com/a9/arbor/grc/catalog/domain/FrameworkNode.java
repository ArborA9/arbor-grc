package com.a9.arbor.grc.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "framework_node", uniqueConstraints = @UniqueConstraint(
        name = "uq_framework_node_code", columnNames = {"framework_id", "node_code"}))
@Getter
@Setter
public class FrameworkNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "framework_id", nullable = false)
    private Long frameworkId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private int level;

    @Column(name = "node_code", nullable = false, length = 64)
    private String nodeCode;

    @Column(nullable = false, length = 512)
    private String title;

    @Lob
    @Column(name = "definition_text")
    private String definitionText;

    @Column(name = "control_type", length = 64)
    private String controlType;

    @Column(length = 8)
    private String priority;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "record_type", length = 32)
    private String recordType;

    @Column(name = "official_ref", length = 128)
    private String officialRef;

    /**
     * Module routing flags from the framework Excel. Until the new parsers land they mirror the legacy
     * control heuristic (definition text present OR level ≥ 3); no consumer reads them yet.
     */
    @Column(name = "in_soa", nullable = false)
    private boolean inSoa;

    @Column(name = "in_gap_assessment", nullable = false)
    private boolean inGapAssessment;

    @Column(name = "in_audit_checklist", nullable = false)
    private boolean inAuditChecklist;

    @Column(name = "in_risk_register", nullable = false)
    private boolean inRiskRegister;

    /** Soft-retire: hidden from new SoA/Gap seeds; kept for in-flight assessments. */
    @Column(name = "retired_at")
    private java.time.Instant retiredAt;

    public boolean isRetired() {
        return retiredAt != null;
    }
}
