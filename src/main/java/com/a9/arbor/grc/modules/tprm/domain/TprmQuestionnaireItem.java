package com.a9.arbor.grc.modules.tprm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tprm_questionnaire_item")
@Getter
@Setter
public class TprmQuestionnaireItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "questionnaire_id", nullable = false)
    private Long questionnaireId;

    @Column(name = "control_id")
    private Long controlId;

    @Column(name = "control_code", length = 40)
    private String controlCode;

    @Column(length = 80)
    private String domain;

    @Column(length = 2000)
    private String requirement;

    @Column(name = "required_evidence", length = 2000)
    private String requiredEvidence;

    @Column(name = "evidence_mandatory", nullable = false)
    private boolean evidenceMandatory;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
