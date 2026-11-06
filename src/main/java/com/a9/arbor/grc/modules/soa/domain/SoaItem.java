package com.a9.arbor.grc.modules.soa.domain;

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
@Table(name = "soa_item")
@Getter
@Setter
public class SoaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "soa_id", nullable = false)
    private Long soaId;

    @Column(name = "framework_node_id", nullable = false)
    private Long frameworkNodeId;

    @Column(name = "node_code", nullable = false, length = 64)
    private String nodeCode;

    @Column(nullable = false, length = 512)
    private String title;

    /** Snapshot at SoA create: catalog edits do not change in-progress SoAs. */
    @Lob
    @Column(name = "definition_text")
    private String definitionText;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private SoaApplicability applicability;

    @Lob
    private String justification;

    @Lob
    private String remarks;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
