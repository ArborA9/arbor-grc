package com.a9.arbor.grc.modules.auditmgmt.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "grc_audit_team_member")
@IdClass(GrcAuditTeamMember.Pk.class)
@Getter
@Setter
public class GrcAuditTeamMember {

    @Id
    @Column(name = "audit_id")
    private Long auditId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_role", nullable = false, length = 16)
    private EngagementTeamRole teamRole;

    @Getter
    @Setter
    public static class Pk implements Serializable {
        private Long auditId;
        private Long userId;
    }
}
