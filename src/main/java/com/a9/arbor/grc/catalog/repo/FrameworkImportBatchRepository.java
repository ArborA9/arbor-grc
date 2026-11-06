package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.FrameworkImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FrameworkImportBatchRepository extends JpaRepository<FrameworkImportBatch, Long> {

    List<FrameworkImportBatch> findTop50ByOrderByStartedAtDescIdDesc();

    List<FrameworkImportBatch> findByFrameworkIdOrderByStartedAtDesc(Long frameworkId);

    void deleteByFrameworkId(Long frameworkId);
}
