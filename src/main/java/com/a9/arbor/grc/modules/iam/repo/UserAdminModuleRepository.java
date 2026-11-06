package com.a9.arbor.grc.modules.iam.repo;

import com.a9.arbor.grc.modules.iam.domain.UserAdminModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAdminModuleRepository extends JpaRepository<UserAdminModule, UserAdminModule.Pk> {

    List<UserAdminModule> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
