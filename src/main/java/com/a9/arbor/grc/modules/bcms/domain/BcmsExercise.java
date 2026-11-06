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
@Table(name = "bcms_exercise")
@Getter
@Setter
public class BcmsExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id")
    private Long projectId;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false, length = 32)
    private ExerciseType exerciseType;

    @Lob
    private String scope;

    @Column(name = "facilitator_user_id")
    private Long facilitatorUserId;

    @Lob
    private String objectives;

    @Lob
    @Column(name = "scenarios_tested")
    private String scenariosTested;

    @Lob
    private String results;

    @Lob
    @Column(name = "gaps_identified")
    private String gapsIdentified;

    @Lob
    @Column(name = "lessons_learned")
    private String lessonsLearned;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "due_notified_at")
    private Instant dueNotifiedAt;

    @Column(name = "overdue_notified_at")
    private Instant overdueNotifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExerciseStatus status = ExerciseStatus.PLANNED;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

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
