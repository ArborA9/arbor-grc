package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.SuperAdminReset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuperAdminResetRepository extends JpaRepository<SuperAdminReset, Long> {

    boolean existsByCodeHash(String codeHash);
}
