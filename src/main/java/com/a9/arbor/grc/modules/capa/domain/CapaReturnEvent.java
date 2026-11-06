package com.a9.arbor.grc.modules.capa.domain;

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
@Table(name = "capa_return_event")
@Getter
@Setter
public class CapaReturnEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "capa_id", nullable = false)
    private Long capaId;

    @Column(name = "rejected_at", nullable = false)
    private Instant rejectedAt;

    @Column(name = "rejected_by_user_id", nullable = false)
    private Long rejectedByUserId;

    @Lob
    @Column(nullable = false)
    private String reason;

    @PrePersist
    void onCreate() {
        if (rejectedAt == null) {
            rejectedAt = Instant.now();
        }
    }
}
