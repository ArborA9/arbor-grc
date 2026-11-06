package com.a9.arbor.grc.modules.org.repo;

import com.a9.arbor.grc.modules.org.domain.OrgUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrgUnitRepository extends JpaRepository<OrgUnit, Long> {
    List<OrgUnit> findAllByOrderByLevelNumAscNameAsc();

    Optional<OrgUnit> findFirstByActiveTrueAndNameIgnoreCase(String name);

    long countByActiveTrue();

    long countByParentIdAndActiveTrue(Long parentId);

    List<OrgUnit> findByParentId(Long parentId);

    List<OrgUnit> findByParentIdIsNull();
}
