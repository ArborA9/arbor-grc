package com.a9.arbor.grc.modules.privacy.repo;

import com.a9.arbor.grc.modules.privacy.domain.PrivacyRopa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivacyRopaRepository extends JpaRepository<PrivacyRopa, Long> {

    List<PrivacyRopa> findAllByOrderByUpdatedAtDesc();

    List<PrivacyRopa> findByRopaDepartmentIdOrderByCreatedAtAsc(Long departmentId);

    long countByDisplayIdStartingWith(String prefix);
}
