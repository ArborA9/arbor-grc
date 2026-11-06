package com.a9.arbor.grc.modules.evidence.web.dto;

import com.a9.arbor.grc.modules.evidence.domain.EvidenceLink;

import java.time.Instant;

public record EvidenceLinkResponse(
        long id,
        String moduleCode,
        String recordType,
        String recordId,
        String workflowStage,
        long linkedByUserId,
        Instant linkedAt
) {
    public static EvidenceLinkResponse from(EvidenceLink link) {
        return new EvidenceLinkResponse(
                link.getId(),
                link.getModuleCode(),
                link.getRecordType(),
                link.getRecordId(),
                link.getWorkflowStage(),
                link.getLinkedByUserId(),
                link.getLinkedAt()
        );
    }
}
