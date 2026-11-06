package com.a9.arbor.grc.modules.notification.domain;

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

/** Email outbox: always persisted; delivery mode controls external send (OUTBOX/SMTP/ACS). */
@Entity
@Table(name = "notification_email_outbox")
@Getter
@Setter
public class NotificationEmailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trigger_type", nullable = false, length = 64)
    private String triggerType;

    @Column(name = "to_email", nullable = false)
    private String toEmail;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "body", nullable = false, length = 8000)
    private String body;

    @Column(name = "body_html", columnDefinition = "CLOB")
    private String bodyHtml;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
