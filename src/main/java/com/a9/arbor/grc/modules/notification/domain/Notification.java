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

@Entity
@Table(name = "notification")
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "trigger_type", nullable = false, length = 64)
    private String triggerType;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "body", length = 2000)
    private String body;

    @Column(name = "record_type", length = 64)
    private String recordType;

    @Column(name = "record_id", length = 64)
    private String recordId;

    @Column(name = "link", length = 500)
    private String link;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isUnread() {
        return readAt == null;
    }
}
