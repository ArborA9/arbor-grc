package com.a9.arbor.grc.modules.asset.repo;

import com.a9.arbor.grc.modules.asset.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findAllByOrderByUpdatedAtDesc();

    List<Asset> findByOrgUnitIdInOrderByUpdatedAtDesc(Collection<Long> orgUnitIds);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
