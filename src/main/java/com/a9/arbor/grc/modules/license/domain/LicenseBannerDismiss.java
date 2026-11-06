package com.a9.arbor.grc.modules.license.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "license_banner_dismiss", uniqueConstraints = @UniqueConstraint(
        name = "uq_license_banner_dismiss", columnNames = {"user_id", "banner_key"}))
@Getter
@Setter
public class LicenseBannerDismiss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "banner_key", nullable = false, length = 128)
    private String bannerKey;

    @Column(name = "dismissed_at", nullable = false)
    private Instant dismissedAt;

    @PrePersist
    void onCreate() {
        if (dismissedAt == null) {
            dismissedAt = Instant.now();
        }
    }
}
