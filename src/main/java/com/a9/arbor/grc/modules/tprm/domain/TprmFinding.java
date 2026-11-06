package com.a9.arbor.grc.modules.tprm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tprm_finding")
@Getter
@Setter
public class TprmFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "assessment_id", nullable = false)
    private Long assessmentId;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "control_code", nullable = false, length = 16)
    private String controlCode;

    @Column(nullable = false, length = 80)
    private String domain;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FindingStatus status = FindingStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
