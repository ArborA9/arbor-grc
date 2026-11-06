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

@Entity
@Table(name = "tprm_vendor_token")
@Getter
@Setter
public class TprmVendorToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_id", nullable = false)
    private Long assessmentId;

    @Column(name = "invitee_email", nullable = false, length = 255)
    private String inviteeEmail;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    /**
     * Raw invite token so Return-to-vendor can re-send the same portal URL.
     * Admin GET still hides this while outbound email is live.
     */
    @Column(name = "token_preview", length = 512)
    private String tokenPreview;

    @Column(name = "otp_hash", nullable = false, length = 128)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VendorTokenStatus status = VendorTokenStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
