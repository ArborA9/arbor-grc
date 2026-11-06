package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.CrossFrameworkMapping;
import com.a9.arbor.grc.catalog.domain.CrossFrameworkMappingType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrossFrameworkMappingRepository extends JpaRepository<CrossFrameworkMapping, Long> {

    List<CrossFrameworkMapping> findBySourceNodeId(Long sourceNodeId);

    List<CrossFrameworkMapping> findBySourceNodeIdAndMappingTypeIn(
            Long sourceNodeId, List<CrossFrameworkMappingType> types);

    List<CrossFrameworkMapping> findBySourceNodeIdOrTargetNodeId(Long sourceNodeId, Long targetNodeId);
}
