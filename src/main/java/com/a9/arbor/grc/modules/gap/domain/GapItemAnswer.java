package com.a9.arbor.grc.modules.gap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "gap_item_answer")
@Getter
@Setter
public class GapItemAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gap_item_id", nullable = false)
    private Long gapItemId;

    @Column(name = "framework_question_id")
    private Long frameworkQuestionId;

    @Column(name = "custom_question_id")
    private Long customQuestionId;

    @Column(name = "question_version", nullable = false)
    private int questionVersion;

    @Column(length = 32)
    private String answer;

    @Lob
    private String note;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
