package com.a9.arbor.grc.modules.evidence.domain;

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

@Entity
@Table(name = "evidence_link")
@Getter
@Setter
public class EvidenceLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evidence_id", nullable = false)
    private Long evidenceId;

    /** Commercial module code or PLATFORM for always-on surfaces. */
    @Column(nullable = false, length = 32)
    private String moduleCode;

    @Column(nullable = false, length = 64)
    private String recordType;

    @Column(nullable = false, length = 64)
    private String recordId;

    @Column(length = 64)
    private String workflowStage;

    @Column(name = "linked_by_user_id", nullable = false)
    private Long linkedByUserId;

    @Column(nullable = false)
    private Instant linkedAt;

    @PrePersist
    void onCreate() {
        linkedAt = Instant.now();
    }
}
