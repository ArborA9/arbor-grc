package com.a9.arbor.grc.modules.auditmgmt.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_access_log")
@Getter
@Setter
public class AuditAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grant_id", nullable = false)
    private Long grantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt = Instant.now();

    @Column(name = "module_code", nullable = false, length = 40)
    private String moduleCode;

    @Column(name = "record_ref", nullable = false, length = 60)
    private String recordRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAccessLogAction action;

    @Column(name = "document_name", length = 250)
    private String documentName;

    @Column(length = 30)
    private String classification;

    @Column(name = "within_scope", nullable = false, length = 32)
    private String withinScope = "OK";
}
