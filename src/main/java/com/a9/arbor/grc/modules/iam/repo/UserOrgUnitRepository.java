package com.a9.arbor.grc.modules.iam.repo;

import com.a9.arbor.grc.modules.iam.domain.UserOrgUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface UserOrgUnitRepository extends JpaRepository<UserOrgUnit, UserOrgUnit.Pk> {
    List<UserOrgUnit> findByUserId(Long userId);

    List<UserOrgUnit> findByOrgUnitId(Long orgUnitId);

    @Modifying
    void deleteByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
