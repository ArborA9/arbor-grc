package com.a9.arbor.grc.modules.bcms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "bcms_dept_profile_asset")
@IdClass(BcmsDeptProfileAsset.Pk.class)
@Getter
@Setter
public class BcmsDeptProfileAsset {

    @Id
    @Column(name = "profile_id")
    private Long profileId;

    @Id
    @Column(name = "asset_id")
    private Long assetId;

    @Getter
    @Setter
    public static class Pk implements Serializable {
        private Long profileId;
        private Long assetId;
    }
}
