package com.a9.arbor.grc.modules.bcms.repo;

import com.a9.arbor.grc.modules.bcms.domain.BiaImpactCell;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BiaImpactCellRepository extends JpaRepository<BiaImpactCell, Long> {
    List<BiaImpactCell> findByProcessId(Long processId);
    void deleteByProcessId(Long processId);
}
