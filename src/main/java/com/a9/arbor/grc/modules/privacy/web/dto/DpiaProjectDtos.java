package com.a9.arbor.grc.modules.privacy.web.dto;

import com.a9.arbor.grc.modules.privacy.domain.DpiaDepartmentStatus;
import com.a9.arbor.grc.modules.privacy.domain.DpiaHeadOutcome;
import com.a9.arbor.grc.modules.privacy.domain.DpiaProjectStatus;
import com.a9.arbor.grc.modules.privacy.domain.DpiaResponseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class DpiaProjectDtos {

    private DpiaProjectDtos() {
    }

    public record CreateProjectRequest(
            @NotBlank @Size(max = 255) String name,
            Integer year,
            Long linkedRopaProjectId) {
    }

    public record AddDepartmentRequest(
            @NotNull Long orgUnitId,
            @Size(max = 4000) String reasonDescription,
            Long departmentHeadUserId,
            Long championUserId,
            LocalDate dueDate,
            Long questionnaireId) {
    }

    public record AssignDepartmentRequest(
            Long departmentHeadUserId,
            Long championUserId,
            LocalDate dueDate,
            Long questionnaireId) {
    }

    public record AnswerItem(
            @NotNull Long questionId,
            String answerText,
            List<String> answerOptions,
            String detailsEvidence) {
    }

    public record SaveAnswersRequest(List<AnswerItem> answers) {
    }

    public record ReviewRequest(
            @NotBlank String outcome,
            String comments) {
    }

    public record AssessmentRowUpdate(
            @NotNull Long id,
            String complianceStatus,
            String remarks,
            String recommendedMeasures,
            String riskTitle) {
    }

    public record SaveAssessmentRequest(List<AssessmentRowUpdate> rows) {
    }

    public record HeadDecideRequest(String notes) {
    }

    public record DecideRequest(String notes) {
    }

    public record InstanceQuestionResponse(
            Long id,
            String code,
            String sectionName,
            String principle,
            String questionText,
            String guidance,
            DpiaResponseType responseType,
            List<String> options,
            int sortOrder,
            boolean required,
            String answerText,
            List<String> answerOptions,
            String detailsEvidence) {
    }

    public record AssessmentRowResponse(
            Long id,
            int sortOrder,
            String principle,
            String descriptionText,
            String complianceStatus,
            String remarks,
            String recommendedMeasures,
            String riskTitle,
            Long raisedRiskId,
            String raisedRiskDisplayId) {
    }

    /** Privacy Risk create picker + DPIA Create Risk button. */
    public record RiskTitleCandidate(
            Long assessmentRowId,
            String riskTitle,
            String principle,
            Long departmentId,
            String departmentName,
            Long projectId,
            String projectName,
            Long orgUnitId,
            Long raisedRiskId,
            String raisedRiskDisplayId) {
    }

    public record DepartmentResponse(
            Long id,
            Long dpiaProjectId,
            Long orgUnitId,
            String orgUnitName,
            String reasonDescription,
            Long questionnaireId,
            String questionnaireName,
            Long departmentHeadUserId,
            String departmentHeadName,
            Long championUserId,
            String championName,
            LocalDate dueDate,
            DpiaDepartmentStatus status,
            String returnFrom,
            String returnReason,
            Long workTaskId,
            Long headWorkTaskId,
            String headNotes,
            Instant headDecidedAt,
            DpiaHeadOutcome headOutcome,
            boolean assessmentComplete,
            Instant closedAt,
            Instant createdAt,
            Instant updatedAt,
            List<InstanceQuestionResponse> questions,
            List<AssessmentRowResponse> assessmentRows) {
    }

    public record ProjectResponse(
            Long id,
            String name,
            Integer year,
            DpiaProjectStatus status,
            Long linkedRopaProjectId,
            String linkedRopaProjectName,
            List<DepartmentResponse> departments,
            Long createdByUserId,
            Instant createdAt,
            Instant updatedAt) {
    }
}
