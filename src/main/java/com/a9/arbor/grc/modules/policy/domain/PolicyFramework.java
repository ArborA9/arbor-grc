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
@Table(name = "policy_framework")
@IdClass(PolicyFramework.Pk.class)
@Getter
@Setter
public class PolicyFramework {

    @Id
    @Column(name = "policy_id")
    private Long policyId;

    @Id
    @Column(name = "framework_id")
    private Long frameworkId;

    @Column(name = "framework_code", nullable = false, length = 64)
    private String frameworkCode;

    @Column(name = "framework_name", nullable = false, length = 255)
    private String frameworkName;

    @Getter
    @Setter
    public static class Pk implements Serializable {
        private Long policyId;
        private Long frameworkId;
    }
}
