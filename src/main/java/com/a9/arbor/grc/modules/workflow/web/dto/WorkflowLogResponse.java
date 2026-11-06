package com.a9.arbor.grc.modules.workflow.web.dto;

import com.a9.arbor.grc.modules.workflow.domain.WorkflowTransitionLog;

import java.time.Instant;

public record WorkflowLogResponse(
        long id,
        String fromState,
        String toState,
        String actionCode,
        long actorUserId,
        String commentText,
        Instant createdAt
) {
    public static WorkflowLogResponse from(WorkflowTransitionLog log) {
        return new WorkflowLogResponse(
                log.getId(),
                log.getFromState(),
                log.getToState(),
                log.getActionCode(),
                log.getActorUserId(),
                log.getCommentText(),
                log.getCreatedAt()
        );
    }
}
