package com.a9.arbor.grc.modules.bcms.repo;

import com.a9.arbor.grc.modules.bcms.domain.BcmsDeptProfileAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BcmsDeptProfileAssetRepository extends JpaRepository<BcmsDeptProfileAsset, BcmsDeptProfileAsset.Pk> {
    List<BcmsDeptProfileAsset> findByProfileId(Long profileId);
    void deleteByProfileIdAndAssetId(Long profileId, Long assetId);
    void deleteByProfileId(Long profileId);
}
