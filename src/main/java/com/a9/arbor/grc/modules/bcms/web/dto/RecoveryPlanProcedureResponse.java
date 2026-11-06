package com.a9.arbor.grc.modules.bcms.web.dto;

import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlanProcedure;

public record RecoveryPlanProcedureResponse(
        Long id,
        Long biaId,
        String processName,
        String procedureText,
        int sortOrder
) {
    public static RecoveryPlanProcedureResponse from(RecoveryPlanProcedure p) {
        return new RecoveryPlanProcedureResponse(
                p.getId(), p.getBiaId(), p.getProcessName(), p.getProcedureText(), p.getSortOrder());
    }
}
