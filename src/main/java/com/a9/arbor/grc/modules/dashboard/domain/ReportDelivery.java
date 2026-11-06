package com.a9.arbor.grc.modules.dashboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "report_delivery")
@Getter
@Setter
public class ReportDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "report_id", nullable = false, length = 64)
    private String reportId;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Lob
    @Column(name = "file_bytes", nullable = false)
    private byte[] fileBytes;

    @Column(name = "delivered_at", nullable = false, updatable = false)
    private Instant deliveredAt;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @PrePersist
    void onCreate() {
        deliveredAt = Instant.now();
    }
}
