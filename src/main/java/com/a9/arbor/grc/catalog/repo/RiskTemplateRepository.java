package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.RiskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RiskTemplateRepository extends JpaRepository<RiskTemplate, Long> {

    List<RiskTemplate> findByFrameworkIdAndActiveTrueOrderByDisplayOrderAscSourceRiskIdAsc(Long frameworkId);

    @Query("select coalesce(max(t.versionNumber), 0) from RiskTemplate t where t.frameworkId = :frameworkId")
    int maxVersion(Long frameworkId);

    long countByFrameworkIdAndActiveTrue(Long frameworkId);
}
