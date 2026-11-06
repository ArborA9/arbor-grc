package com.a9.arbor.grc.modules.iam.repo;

import com.a9.arbor.grc.modules.iam.domain.SsoGroupRoleMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SsoGroupRoleMappingRepository extends JpaRepository<SsoGroupRoleMapping, Long> {

    List<SsoGroupRoleMapping> findAllByOrderByIdpGroupNameAsc();

    List<SsoGroupRoleMapping> findByIdpGroupNameIn(Collection<String> idpGroupNames);
}
