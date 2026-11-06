package com.a9.arbor.grc.modules.bcms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "recovery_plan_procedure")
@Getter
@Setter
public class RecoveryPlanProcedure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "bia_id")
    private Long biaId;

    @Column(name = "process_name", nullable = false, length = 255)
    private String processName;

    @Lob
    @Column(name = "procedure_text")
    private String procedureText;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
