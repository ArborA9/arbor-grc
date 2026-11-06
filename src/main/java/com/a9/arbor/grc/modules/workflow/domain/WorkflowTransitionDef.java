package com.a9.arbor.grc.modules.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "workflow_transition_def")
@Getter
@Setter
public class WorkflowTransitionDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "definition_id", nullable = false)
    private Long definitionId;

    @Column(name = "from_state", nullable = false, length = 64)
    private String fromState;

    @Column(name = "to_state", nullable = false, length = 64)
    private String toState;

    @Column(name = "action_code", nullable = false, length = 64)
    private String actionCode;

    /** Comma-separated Role names, e.g. OWNER,ADMIN */
    @Column(name = "allowed_roles", nullable = false, length = 255)
    private String allowedRoles;

    @Column(name = "requires_comment", nullable = false)
    private boolean requiresComment;
}
