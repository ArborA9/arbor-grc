package com.a9.arbor.grc.modules.bcms.web.dto;

import com.a9.arbor.grc.modules.bcms.domain.BiaCycle;
import com.a9.arbor.grc.modules.bcms.domain.BiaStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BiaCycleResponse(
        Long id,
        Long biaProjectId,
        Long orgUnitId,
        String displayId,
        LocalDate issueDate,
        LocalDate nextReviewDate,
        String versionLabel,
        String objective,
        String scopeText,
        String mbco,
        BiaStatus status,
        Long assignedToUserId,
        LocalDate dueDate,
        Long workTaskId,
        String returnReason,
        Instant approvedAt,
        Long approvedByUserId,
        Long bcHeadUserId,
        String bcHeadNotes,
        Instant bcHeadDecidedAt,
        String adminForwardNotes,
        Instant closedAt,
        Long closedByUserId,
        Long bcHeadWorkTaskId,
        Long createdByUserId,
        Instant createdAt,
        Instant updatedAt,
        BiaKpiResponse kpis,
        List<BiaRankedRowResponse> rankedCritical,
        List<BiaProcessResponse> processes
) {
    public static BiaCycleResponse from(
            BiaCycle c,
            BiaKpiResponse kpis,
            List<BiaRankedRowResponse> ranked,
            List<BiaProcessResponse> processes) {
        return new BiaCycleResponse(
                c.getId(), c.getBiaProjectId(), c.getOrgUnitId(), c.getDisplayId(), c.getIssueDate(),
                c.getNextReviewDate(), c.getVersionLabel(), c.getObjective(), c.getScopeText(), c.getMbco(),
                c.getStatus(), c.getAssignedToUserId(), c.getDueDate(), c.getWorkTaskId(), c.getReturnReason(),
                c.getApprovedAt(), c.getApprovedByUserId(), c.getBcHeadUserId(), c.getBcHeadNotes(),
                c.getBcHeadDecidedAt(), c.getAdminForwardNotes(), c.getClosedAt(), c.getClosedByUserId(),
                c.getBcHeadWorkTaskId(), c.getCreatedByUserId(), c.getCreatedAt(), c.getUpdatedAt(),
                kpis, ranked, processes);
    }
}
