package com.a9.arbor.grc.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "framework_question")
@Getter
@Setter
public class FrameworkQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "framework_id", nullable = false)
    private Long frameworkId;

    @Column(name = "framework_node_id", nullable = false)
    private Long frameworkNodeId;

    @Column(name = "question_code", nullable = false, length = 64)
    private String questionCode;

    /** Control ref the question hangs off, and its ordinal within that control (Q1..Qn repeat per control). */
    @Column(name = "control_code", nullable = false, length = 64)
    private String controlCode;

    @Column(name = "question_number", nullable = false, length = 64)
    private String questionNumber;

    /**
     * The ref the client authored the question against, when that differs from {@link #controlCode} -
     * set only by the reference-pack import, where a row deeper than the Master hierarchy attaches to
     * its nearest ancestor. Null means "authored against control_code".
     */
    @Column(name = "source_ref", length = 64)
    private String sourceRef;

    @Column(name = "answer_options", length = 255)
    private String answerOptions;

    @Column(name = "is_mandatory", nullable = false)
    private boolean mandatory;

    @Column(name = "question_type", length = 64)
    private String questionType;

    @Lob
    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Lob
    @Column(name = "evidence_guidance")
    private String evidenceGuidance;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
