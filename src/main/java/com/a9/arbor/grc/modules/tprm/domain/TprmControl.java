package com.a9.arbor.grc.modules.tprm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tprm_control")
@Getter
@Setter
public class TprmControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "control_code", nullable = false, length = 40)
    private String controlCode;

    @Column(nullable = false, length = 80)
    private String domain;

    @Column(nullable = false, length = 500)
    private String requirement;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(name = "required_evidence", nullable = false, length = 2000)
    private String requiredEvidence;

    @Column(nullable = false)
    private int weight;

    @Column(name = "weight_label", nullable = false, length = 32)
    private String weightLabel;

    @Column(nullable = false)
    private boolean mandatory;

    @Column(name = "asked_from_tier", nullable = false)
    private int askedFromTier;

    @Column(length = 120)
    private String iso27001;

    @Column(name = "uae_ia", length = 120)
    private String uaeIa;

    @Column(name = "other_standard", length = 120)
    private String otherStandard;

    @Column(nullable = false, length = 16)
    private String module;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
