package com.a9.arbor.grc.modules.policy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "policy_acknowledgement")
@Getter
@Setter
public class PolicyAcknowledgement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_version_id", nullable = false)
    private Long policyVersionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "acknowledged_at", nullable = false)
    private Instant acknowledgedAt;

    @PrePersist
    void onCreate() {
        if (acknowledgedAt == null) {
            acknowledgedAt = Instant.now();
        }
    }
}
