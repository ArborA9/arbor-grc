package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementTeamRole;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditTeamMember;

public record AuditTeamMemberResponse(
        Long userId,
        EngagementTeamRole teamRole
) {
    public static AuditTeamMemberResponse from(GrcAuditTeamMember row) {
        return new AuditTeamMemberResponse(row.getUserId(), row.getTeamRole());
    }
}
