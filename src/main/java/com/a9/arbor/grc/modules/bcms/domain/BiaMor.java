package com.a9.arbor.grc.modules.bcms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bia_mor")
@Getter
@Setter
public class BiaMor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_id", nullable = false)
    private Long processId;

    @Column(name = "capacity_pct")
    private Integer capacityPct;

    @Column(name = "min_staff")
    private Integer minStaff;

    @Column(length = 4000)
    private String people;

    @Column(length = 4000)
    private String technology;

    @Column(length = 4000)
    private String premises;

    @Column(length = 4000)
    private String equipment;

    @Column(name = "records_text", length = 4000)
    private String recordsText;

    @Column(name = "third_party_support", length = 4000)
    private String thirdPartySupport;

    @Column(name = "recovery_strategy", length = 255)
    private String recoveryStrategy;

    @Column(length = 4000)
    private String workaround;

    @Column(name = "min_staff_people", length = 8000)
    private String minStaffPeople;

    @Column(name = "recovery_strategies", length = 8000)
    private String recoveryStrategies;

    @Column(name = "internet_yn", length = 8)
    private String internetYn;

    @Column(name = "intranet_yn", length = 8)
    private String intranetYn;

    @Column(name = "vpn_yn", length = 8)
    private String vpnYn;

    @Column(name = "site_type", length = 32)
    private String siteType;

    @Column(name = "software_apps", length = 8000)
    private String softwareApps;

    private Integer laptops;

    @Column(name = "m365_yn", length = 8)
    private String m365Yn;

    @Column(name = "third_party_contacts", length = 8000)
    private String thirdPartyContacts;
}
