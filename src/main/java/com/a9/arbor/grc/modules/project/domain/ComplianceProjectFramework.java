package com.a9.arbor.grc.modules.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "compliance_project_framework")
@Getter
@Setter
public class ComplianceProjectFramework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "framework_id", nullable = false)
    private Long frameworkId;

    @Column(name = "framework_code", nullable = false, length = 64)
    private String frameworkCode;

    @Column(name = "framework_name", nullable = false)
    private String frameworkName;

    @Column(name = "framework_version", nullable = false, length = 64)
    private String frameworkVersion;
}
