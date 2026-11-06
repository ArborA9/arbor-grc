package com.a9.arbor.grc.modules.task.web.dto;

import com.a9.arbor.grc.catalog.E2eCatalogMarker;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;

import java.time.Instant;
import java.time.LocalDate;

public record WorkTaskSummaryResponse(
        Long id,
        String sourceModule,
        String sourceRecordType,
        Long sourceRecordId,
        String title,
        String detail,
        WorkTaskStatus status,
        LocalDate dueDate,
        Long assignedByUserId,
        String assignedByName,
        String assignedByEmail,
        Instant submittedAt,
        Instant updatedAt,
        boolean returned,
        String returnReason,
        Long projectId,
        String projectName,
        String riskType,
        String riskStatus
) {
    public static WorkTaskSummaryResponse from(WorkTask t, User assignedBy) {
        return from(t, assignedBy, false, null, null, null, null, null);
    }

    public static WorkTaskSummaryResponse from(WorkTask t, User assignedBy, boolean returned, String returnReason) {
        return from(t, assignedBy, returned, returnReason, null, null, null, null);
    }

    public static WorkTaskSummaryResponse from(WorkTask t, User assignedBy, boolean returned, String returnReason,
                                               Long projectId, String projectName) {
        return from(t, assignedBy, returned, returnReason, projectId, projectName, null, null);
    }

    public static WorkTaskSummaryResponse from(WorkTask t, User assignedBy, boolean returned, String returnReason,
                                               Long projectId, String projectName, String riskType) {
        return from(t, assignedBy, returned, returnReason, projectId, projectName, riskType, null);
    }

    public static WorkTaskSummaryResponse from(WorkTask t, User assignedBy, boolean returned, String returnReason,
                                               Long projectId, String projectName, String riskType, String riskStatus) {
        return new WorkTaskSummaryResponse(
                t.getId(),
                t.getSourceModule(),
                t.getSourceRecordType(),
                t.getSourceRecordId(),
                E2eCatalogMarker.strip(t.getTitle()),
                E2eCatalogMarker.strip(t.getDetail()),
                t.getStatus(),
                t.getDueDate(),
                t.getAssignedByUserId(),
                assignedBy != null ? assignedBy.getFullName() : null,
                assignedBy != null ? assignedBy.getEmail() : null,
                t.getSubmittedAt(),
                t.getUpdatedAt(),
                returned,
                returnReason,
                projectId,
                projectName,
                riskType,
                riskStatus);
    }
}
