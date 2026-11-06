package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.FrameworkImportRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FrameworkImportRowRepository extends JpaRepository<FrameworkImportRow, Long> {

    List<FrameworkImportRow> findByBatchIdOrderByRowIndexAsc(Long batchId);

    long countByBatchIdAndStatus(Long batchId, String status);
}
