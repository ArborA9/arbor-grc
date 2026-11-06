package com.a9.arbor.grc.modules.org.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "org_level_config")
@Getter
@Setter
public class OrgLevelConfig {

    @Id
    @Column(name = "level_num")
    private Integer levelNum;

    @Column(nullable = false, length = 64)
    private String label;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
