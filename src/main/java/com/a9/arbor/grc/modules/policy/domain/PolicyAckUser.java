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
@Table(name = "policy_ack_user")
@IdClass(PolicyAckUser.Pk.class)
@Getter
@Setter
public class PolicyAckUser {

    @Id
    @Column(name = "policy_id")
    private Long policyId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Getter
    @Setter
    public static class Pk implements Serializable {
        private Long policyId;
        private Long userId;
    }
}
