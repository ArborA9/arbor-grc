package com.a9.arbor.grc.modules.org.repo;

import com.a9.arbor.grc.modules.org.domain.OrgLevelConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrgLevelConfigRepository extends JpaRepository<OrgLevelConfig, Integer> {
    List<OrgLevelConfig> findAllByOrderByLevelNumAsc();
}
