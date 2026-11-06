package com.a9.arbor.grc.modules.bcms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "recovery_plan_bia")
@IdClass(RecoveryPlanBia.Pk.class)
@Getter
@Setter
public class RecoveryPlanBia {

    @Id
    @Column(name = "plan_id")
    private Long planId;

    @Id
    @Column(name = "bia_id")
    private Long biaId;

    @Getter
    @Setter
    public static class Pk implements Serializable {
        private Long planId;
        private Long biaId;
    }
}
