package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.CatalogContentAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogContentAuditRepository extends JpaRepository<CatalogContentAudit, Long> {

    List<CatalogContentAudit> findByFrameworkIdOrderByCreatedAtDesc(Long frameworkId);
}
