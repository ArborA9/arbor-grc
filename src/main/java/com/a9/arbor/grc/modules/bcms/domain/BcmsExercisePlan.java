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
@Table(name = "bcms_exercise_plan")
@IdClass(BcmsExercisePlan.Pk.class)
@Getter
@Setter
public class BcmsExercisePlan {

    @Id
    @Column(name = "exercise_id")
    private Long exerciseId;

    @Id
    @Column(name = "plan_id")
    private Long planId;

    @Getter
    @Setter
    public static class Pk implements Serializable {
        private Long exerciseId;
        private Long planId;
    }
}
