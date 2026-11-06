package com.a9.arbor.grc.modules.incident.domain;

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

@Entity
@Table(name = "incident_record")
@Getter
@Setter
public class IncidentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_id", nullable = false, length = 32, unique = true)
    private String displayId;

    @Column(nullable = false, length = 512)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false, length = 32)
    private IncidentType incidentType;

    @Lob
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "affected_area", length = 512)
    private String affectedArea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private IncidentPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private IncidentStatus status;

    @Column(name = "linked_risk_id")
    private Long linkedRiskId;

    /** Doc 02 R-12: optional link to a Gap assessment. */
    @Column(name = "linked_gap_assessment_id")
    private Long linkedGapAssessmentId;

    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Lob
    @Column(name = "root_cause")
    private String rootCause;

    @Lob
    @Column(name = "containment_actions")
    private String containmentActions;

    @Lob
    @Column(name = "resolution_notes")
    private String resolutionNotes;

    @Column(name = "response_due_at", nullable = false)
    private Instant responseDueAt;

    @Column(name = "resolution_due_at")
    private Instant resolutionDueAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by_user_id")
    private Long resolvedByUserId;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by_user_id")
    private Long closedByUserId;

    @Column(name = "overdue_notified_at")
    private Instant overdueNotifiedAt;

    @Column(name = "work_task_id")
    private Long workTaskId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
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
