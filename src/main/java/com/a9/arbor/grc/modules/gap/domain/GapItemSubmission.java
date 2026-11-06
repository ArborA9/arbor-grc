package com.a9.arbor.grc.modules.gap.domain;

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

import java.time.Instant;

@Entity
@Table(name = "gap_item_submission")
@Getter
@Setter
public class GapItemSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gap_item_id", nullable = false)
    private Long gapItemId;

    @Column(name = "submission_number", nullable = false)
    private int submissionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "implementation_status", length = 32)
    private GapImplementationStatus implementationStatus;

    /** SAMA snapshot: null for ISO-family submissions. */
    private Integer maturity;

    /** SAMA snapshot of evidence-gate answers at submit time. */
    @Lob
    @Column(name = "maturity_gates_json")
    private String maturityGatesJson;

    @Lob
    private String observations;

    @Lob
    @Column(name = "gap_description")
    private String gapDescription;

    @Lob
    @Column(name = "contributor_comments")
    private String contributorComments;

    private Integer score;

    @Column(name = "submitted_by_user_id", nullable = false)
    private Long submittedByUserId;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "work_task_id")
    private Long workTaskId;
}
