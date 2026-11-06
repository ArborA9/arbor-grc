package com.a9.arbor.grc.modules.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "work_task")
@Getter
@Setter
public class WorkTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_module", nullable = false, length = 32)
    private String sourceModule;

    @Column(name = "source_record_type", nullable = false, length = 64)
    private String sourceRecordType;

    @Column(name = "source_record_id", nullable = false)
    private Long sourceRecordId;

    @Column(nullable = false, length = 1100)
    private String title;

    @Column(length = 1000)
    private String detail;

    @Column(name = "assigned_to_user_id", nullable = false)
    private Long assignedToUserId;

    @Column(name = "assigned_by_user_id", nullable = false)
    private Long assignedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkTaskStatus status = WorkTaskStatus.PENDING;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "not_responded_notified_at")
    private Instant notRespondedNotifiedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

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
