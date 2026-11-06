package com.a9.arbor.grc.modules.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "workflow_instance")
@Getter
@Setter
public class WorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "definition_id", nullable = false)
    private Long definitionId;

    @Column(name = "module_code", nullable = false, length = 32)
    private String moduleCode;

    @Column(name = "record_type", nullable = false, length = 64)
    private String recordType;

    @Column(name = "record_id", nullable = false, length = 64)
    private String recordId;

    @Column(name = "record_name", length = 500)
    private String recordName;

    @Column(name = "current_state", nullable = false, length = 64)
    private String currentState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkflowInstanceStatus status = WorkflowInstanceStatus.OPEN;

    @Column(name = "started_by_user_id", nullable = false)
    private Long startedByUserId;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;

    @PrePersist
    void onCreate() {
        startedAt = Instant.now();
    }
}
