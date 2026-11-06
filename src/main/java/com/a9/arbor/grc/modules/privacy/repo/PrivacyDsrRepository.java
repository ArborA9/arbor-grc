package com.a9.arbor.grc.modules.privacy.repo;

import com.a9.arbor.grc.modules.privacy.domain.DsrStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyDsr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PrivacyDsrRepository extends JpaRepository<PrivacyDsr, Long> {

    List<PrivacyDsr> findAllByOrderByUpdatedAtDesc();

    List<PrivacyDsr> findByStatusInOrderByDueAtAsc(Collection<DsrStatus> statuses);

    long countByDisplayIdStartingWith(String prefix);
}
