package com.a9.arbor.grc.modules.privacy.domain;

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

@Entity
@Table(name = "privacy_data_inventory")
@Getter
@Setter
public class PrivacyDataInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_id", nullable = false, length = 32)
    private String displayId;

    @Column(name = "data_type", nullable = false, length = 255)
    private String dataType;

    @Column(name = "data_subjects", length = 500)
    private String dataSubjects;

    @Column(length = 255)
    private String volume;

    @Column(length = 255)
    private String format;

    @Column(name = "storage_asset_id")
    private Long storageAssetId;

    @Column(name = "storage_location", length = 500)
    private String storageLocation;

    @Column(name = "storage_country", length = 255)
    private String storageCountry;

    @Column(name = "retention_period", length = 255)
    private String retentionPeriod;

    @Column(name = "deletion_method", length = 255)
    private String deletionMethod;

    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InventoryStatus status = InventoryStatus.ACTIVE;

    @Column(length = 2000)
    private String notes;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

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
