package com.a9.arbor.grc.modules.privacy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dpia_dpo_assessment_row")
@Getter
@Setter
public class DpiaDpoAssessmentRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dpia_department_id", nullable = false)
    private Long dpiaDepartmentId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 255)
    private String principle;

    @Lob
    @Column(name = "description_text", nullable = false)
    private String descriptionText;

    @Column(name = "compliance_status", length = 64)
    private String complianceStatus;

    @Lob
    @Column
    private String remarks;

    @Lob
    @Column(name = "recommended_measures")
    private String recommendedMeasures;

    @Column(name = "risk_title", length = 500)
    private String riskTitle;
}
