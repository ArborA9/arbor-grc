package com.a9.arbor.grc.modules.workflow.web.dto;

import com.a9.arbor.grc.modules.workflow.domain.WorkflowInstance;
import com.a9.arbor.grc.modules.workflow.domain.WorkflowTransitionLog;

import java.time.Instant;
import java.util.List;

public record WorkflowInstanceResponse(
        long id,
        long definitionId,
        String definitionCode,
        String definitionName,
        String moduleCode,
        String recordType,
        String recordId,
        String recordName,
        String currentState,
        String status,
        long startedByUserId,
        Instant startedAt,
        Instant completedAt,
        List<WorkflowTransitionDefResponse> availableActions,
        List<WorkflowLogResponse> history
) {
    public static WorkflowInstanceResponse from(WorkflowInstance i,
                                                String defCode,
                                                String defName,
                                                List<WorkflowTransitionDefResponse> available,
                                                List<WorkflowTransitionLog> history) {
        return new WorkflowInstanceResponse(
                i.getId(),
                i.getDefinitionId(),
                defCode,
                defName,
                i.getModuleCode(),
                i.getRecordType(),
                i.getRecordId(),
                i.getRecordName(),
                i.getCurrentState(),
                i.getStatus().name(),
                i.getStartedByUserId(),
                i.getStartedAt(),
                i.getCompletedAt(),
                available,
                history.stream().map(WorkflowLogResponse::from).toList()
        );
    }
}
