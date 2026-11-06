package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.Framework;
import com.a9.arbor.grc.catalog.domain.FrameworkStatus;
import com.a9.arbor.grc.catalog.domain.ModuleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FrameworkRepository extends JpaRepository<Framework, Long> {

    Optional<Framework> findByCodeAndVersion(String code, String version);

    List<Framework> findAllByOrderByCodeAscVersionDesc();

    List<Framework> findByStatusAndModuleCodeInOrderByCodeAscVersionDesc(
            FrameworkStatus status, Collection<ModuleCode> moduleCodes);
}
