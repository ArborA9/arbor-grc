package com.a9.arbor.grc.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "cross_framework_mapping", uniqueConstraints = @UniqueConstraint(
        name = "uq_cfm_pair", columnNames = {"source_node_id", "target_node_id"}))
@Getter
@Setter
public class CrossFrameworkMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_node_id", nullable = false)
    private Long sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    private Long targetNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_type", nullable = false, length = 32)
    private CrossFrameworkMappingType mappingType;

    @Lob
    private String justification;

    @Column(name = "loaded_at", nullable = false, updatable = false)
    private Instant loadedAt;

    @PrePersist
    void onCreate() {
        if (loadedAt == null) {
            loadedAt = Instant.now();
        }
    }
}
