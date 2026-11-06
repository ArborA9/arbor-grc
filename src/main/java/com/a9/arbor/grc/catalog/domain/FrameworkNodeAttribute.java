package com.a9.arbor.grc.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Free-form per-node columns from the framework workbooks: {@code section_type},
 * {@code is_mandatory}, {@code cross_ref_*}, {@code notes} today, and the reference packs'
 * {@code what_it_means} / {@code evidence_examples} in F3: so new columns land without a migration.
 */
@Entity
@Table(name = "framework_node_attribute", uniqueConstraints = @UniqueConstraint(
        name = "uq_fna_node_key", columnNames = {"node_id", "attr_key"}))
@Getter
@Setter
public class FrameworkNodeAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    @Column(name = "attr_key", nullable = false, length = 64)
    private String attrKey;

    @Lob
    @Column(name = "attr_value")
    private String attrValue;
}
