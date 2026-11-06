package com.a9.arbor.grc.modules.org.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "org_unit")
@Getter
@Setter
public class OrgUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "level_num", nullable = false)
    private int levelNum;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "head_user_id")
    private Long headUserId;

    @Column(nullable = false)
    private boolean active = true;

    /** MANUAL or ENTRA. Level-1 departments synced from Entra keep ENTRA. */
    @Column(nullable = false, length = 16)
    private String source = "MANUAL";

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
