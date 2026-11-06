package com.a9.arbor.grc.modules.bcms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDate;

@Entity
@Table(name = "recovery_plan")
@Getter
@Setter
public class RecoveryPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id")
    private Long projectId;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 32)
    private RecoveryPlanType planType;

    @Lob
    private String scope;

    @Column(name = "version_num", nullable = false)
    private int versionNum = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecoveryPlanStatus status = RecoveryPlanStatus.DRAFT;

    @Lob
    @Column(name = "crisis_team")
    private String crisisTeam;

    @Lob
    @Column(name = "escalation_contacts")
    private String escalationContacts;

    @Lob
    @Column(name = "communication_tree")
    private String communicationTree;

    @Lob
    @Column(name = "resource_requirements")
    private String resourceRequirements;

    @Lob
    @Column(name = "alternate_site_details")
    private String alternateSiteDetails;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Column(name = "review_due_notified_at")
    private Instant reviewDueNotifiedAt;

    @Column(name = "expired_notified_at")
    private Instant expiredNotifiedAt;

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
