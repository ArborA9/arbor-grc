package com.a9.arbor.grc.modules.privacy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dpia_instance_question")
@Getter
@Setter
public class DpiaInstanceQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dpia_department_id", nullable = false)
    private Long dpiaDepartmentId;

    @Column(nullable = false, length = 16)
    private String code;

    @Column(name = "section_name", nullable = false, length = 255)
    private String sectionName;

    @Column(nullable = false, length = 255)
    private String principle;

    @Lob
    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Lob
    @Column
    private String guidance;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_type", nullable = false, length = 64)
    private DpiaResponseType responseType;

    @Lob
    @Column(name = "options_json")
    private String optionsJson;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "required_flag", nullable = false)
    private boolean required = true;

    @Lob
    @Column(name = "answer_text")
    private String answerText;

    @Lob
    @Column(name = "answer_json")
    private String answerJson;

    @Lob
    @Column(name = "details_evidence")
    private String detailsEvidence;
}
