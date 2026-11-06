package com.a9.arbor.grc.modules.workflow.web.dto;

import com.a9.arbor.grc.modules.workflow.domain.WorkflowTransitionDef;

public record WorkflowTransitionDefResponse(
        long id,
        String fromState,
        String toState,
        String actionCode,
        String allowedRoles,
        boolean requiresComment
) {
    public static WorkflowTransitionDefResponse from(WorkflowTransitionDef t) {
        return new WorkflowTransitionDefResponse(
                t.getId(),
                t.getFromState(),
                t.getToState(),
                t.getActionCode(),
                t.getAllowedRoles(),
                t.isRequiresComment()
        );
    }
}
