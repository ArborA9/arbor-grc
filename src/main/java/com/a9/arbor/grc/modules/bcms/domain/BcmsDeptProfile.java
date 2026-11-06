package com.a9.arbor.grc.modules.bcms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "bcms_dept_profile")
@Getter
@Setter
public class BcmsDeptProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false, unique = true)
    private Long orgUnitId;

    @Column(name = "total_staff")
    private Integer totalStaff;

    @Lob
    @Column(name = "critical_staff_roles")
    private String criticalStaffRoles;

    @Lob
    @Column(name = "primary_functions")
    private String primaryFunctions;

    @Lob
    @Column(name = "internal_dependencies")
    private String internalDependencies;

    @Lob
    @Column(name = "external_dependencies")
    private String externalDependencies;

    @Column(name = "physical_location", length = 512)
    private String physicalLocation;

    @Column(name = "alternate_work_location", length = 512)
    private String alternateWorkLocation;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
