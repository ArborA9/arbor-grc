package com.a9.arbor.grc.modules.privacy.web.dto;

import com.a9.arbor.grc.modules.privacy.domain.RopaDepartment;
import com.a9.arbor.grc.modules.privacy.domain.RopaDepartmentStatus;
import com.a9.arbor.grc.modules.privacy.domain.RopaProject;
import com.a9.arbor.grc.modules.privacy.domain.RopaProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class RopaProjectDtos {

    private RopaProjectDtos() {
    }

    public record CreateProjectRequest(@NotBlank @Size(max = 255) String name, Integer year) {
    }

    public record AddDepartmentRequest(
            @NotNull Long orgUnitId,
            @NotNull Long departmentHeadUserId,
            @NotNull Long championUserId) {
    }

    public record AssignDepartmentRequest(
            @NotNull Long departmentHeadUserId,
            @NotNull Long championUserId) {
    }

    /**
     * DPO per-process review. DPIA required is derived from the four Arbor criteria
     * (any Yes → required). {@code dpiaRequired} in the body is ignored if sent.
     */
    public record ReviewProcessRequest(
            Boolean dpiaRequired,
            String dpiaComments,
            String comments,
            @NotBlank String outcome,
            Boolean dpiaCritLargeSensitive,
            Boolean dpiaCritSystematicAssessment,
            Boolean dpiaCritAutomatedProfiling,
            Boolean dpiaCritLegalSeriousImpact) {
    }

    public record DecideRequest(String notes) {
    }

    /** Department Head decision on one process: APPROVED or RETURNED (comment required). */
    public record HeadReviewProcessRequest(
            @NotBlank String outcome,
            String comments) {
    }

    public record DepartmentResponse(
            Long id,
            Long ropaProjectId,
            Long orgUnitId,
            String orgUnitName,
            Long departmentHeadUserId,
            String departmentHeadName,
            Long championUserId,
            String championName,
            LocalDate dueDate,
            RopaDepartmentStatus status,
            String returnFrom,
            String returnReason,
            Long workTaskId,
            Long headWorkTaskId,
            String headNotes,
            Instant headDecidedAt,
            Instant closedAt,
            int processCount,
            Instant createdAt,
            Instant updatedAt) {

        public static DepartmentResponse from(RopaDepartment d, String orgUnitName,
                                              String headName, String championName, int processCount) {
            return new DepartmentResponse(d.getId(), d.getRopaProjectId(), d.getOrgUnitId(), orgUnitName,
                    d.getDepartmentHeadUserId(), headName, d.getChampionUserId(), championName,
                    d.getDueDate(), d.getStatus(), d.getReturnFrom(), d.getReturnReason(),
                    d.getWorkTaskId(), d.getHeadWorkTaskId(), d.getHeadNotes(), d.getHeadDecidedAt(),
                    d.getClosedAt(), processCount, d.getCreatedAt(), d.getUpdatedAt());
        }
    }

    public record ProjectResponse(
            Long id,
            String name,
            Integer year,
            RopaProjectStatus status,
            List<DepartmentResponse> departments,
            Long createdByUserId,
            Instant createdAt,
            Instant updatedAt) {

        public static ProjectResponse from(RopaProject p, List<DepartmentResponse> departments) {
            return new ProjectResponse(p.getId(), p.getName(), p.getYear(), p.getStatus(), departments,
                    p.getCreatedByUserId(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }
}
