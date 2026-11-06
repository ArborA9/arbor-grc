package com.a9.arbor.grc.modules.policy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "policy_ack_org_unit")
@IdClass(PolicyAckOrgUnit.Pk.class)
@Getter
@Setter
public class PolicyAckOrgUnit {

    @Id
    @Column(name = "policy_id")
    private Long policyId;

    @Id
    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Getter
    @Setter
    public static class Pk implements Serializable {
        private Long policyId;
        private Long orgUnitId;
    }
}
