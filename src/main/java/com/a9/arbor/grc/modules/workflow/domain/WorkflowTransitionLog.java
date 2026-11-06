package com.a9.arbor.grc.modules.workflow.domain;

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
@Table(name = "workflow_transition_log")
@Getter
@Setter
public class WorkflowTransitionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instance_id", nullable = false)
    private Long instanceId;

    @Column(name = "from_state", nullable = false, length = 64)
    private String fromState;

    @Column(name = "to_state", nullable = false, length = 64)
    private String toState;

    @Column(name = "action_code", nullable = false, length = 64)
    private String actionCode;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "comment_text", length = 2000)
    private String commentText;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
