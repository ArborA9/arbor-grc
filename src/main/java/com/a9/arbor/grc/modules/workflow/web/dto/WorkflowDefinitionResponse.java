package com.a9.arbor.grc.modules.workflow.web.dto;

import com.a9.arbor.grc.modules.workflow.domain.WorkflowDefinition;
import com.a9.arbor.grc.modules.workflow.domain.WorkflowTransitionDef;

import java.util.List;

public record WorkflowDefinitionResponse(
        long id,
        String code,
        String name,
        String moduleCode,
        String recordType,
        String initialState,
        int version,
        List<WorkflowTransitionDefResponse> transitions
) {
    public static WorkflowDefinitionResponse from(WorkflowDefinition d, List<WorkflowTransitionDef> transitions) {
        return new WorkflowDefinitionResponse(
                d.getId(),
                d.getCode(),
                d.getName(),
                d.getModuleCode(),
                d.getRecordType(),
                d.getInitialState(),
                d.getVersion(),
                transitions.stream().map(WorkflowTransitionDefResponse::from).toList()
        );
    }
}
