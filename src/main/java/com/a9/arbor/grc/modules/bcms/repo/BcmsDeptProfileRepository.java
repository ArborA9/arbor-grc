package com.a9.arbor.grc.modules.bcms.repo;

import com.a9.arbor.grc.modules.bcms.domain.BcmsDeptProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BcmsDeptProfileRepository extends JpaRepository<BcmsDeptProfile, Long> {
    Optional<BcmsDeptProfile> findByOrgUnitId(Long orgUnitId);
    List<BcmsDeptProfile> findAllByOrderByUpdatedAtDesc();
}
