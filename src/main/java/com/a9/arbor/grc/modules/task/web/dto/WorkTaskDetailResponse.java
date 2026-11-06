package com.a9.arbor.grc.modules.task.web.dto;

import com.a9.arbor.grc.modules.bcms.web.dto.BiaCycleResponse;
import com.a9.arbor.grc.modules.capa.web.dto.CapaResponse;
import com.a9.arbor.grc.modules.gap.web.dto.GapItemResponse;
import com.a9.arbor.grc.modules.incident.web.dto.IncidentResponse;
import com.a9.arbor.grc.modules.kri.web.dto.KriResponse;
import com.a9.arbor.grc.modules.policy.web.dto.PolicyTaskResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.DpiaResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.RopaResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskRecordResponse;
import com.a9.arbor.grc.modules.risk.web.dto.TreatmentMilestoneResponse;

public record WorkTaskDetailResponse(
        WorkTaskSummaryResponse task,
        GapItemResponse gapItem,
        Long gapAssessmentId,
        String assessmentName,
        String frameworkCode,
        RiskRecordResponse riskRecord,
        CapaResponse capa,
        BiaCycleResponse bia,
        PolicyTaskResponse policy,
        KriResponse kri,
        IncidentResponse incident,
        TreatmentMilestoneResponse treatmentMilestone,
        RopaResponse privacyRopa,
        DpiaResponse privacyDpia
) {
}
