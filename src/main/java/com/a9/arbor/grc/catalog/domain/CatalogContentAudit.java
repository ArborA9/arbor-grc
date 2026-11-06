package com.a9.arbor.grc.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "catalog_content_audit")
@Getter
@Setter
public class CatalogContentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "framework_id", nullable = false)
    private Long frameworkId;

    @Column(name = "node_id")
    private Long nodeId;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(name = "field_name", length = 64)
    private String fieldName;

    @Lob
    @Column(name = "old_value")
    private String oldValue;

    @Lob
    @Column(name = "new_value")
    private String newValue;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
