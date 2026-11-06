package com.a9.arbor.grc.modules.privacy.repo;

import com.a9.arbor.grc.modules.privacy.domain.BreachStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyBreach;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PrivacyBreachRepository extends JpaRepository<PrivacyBreach, Long> {

    List<PrivacyBreach> findAllByOrderByUpdatedAtDesc();

    List<PrivacyBreach> findByStatusInOrderByReportDeadlineAtAsc(Collection<BreachStatus> statuses);

    long countByDisplayIdStartingWith(String prefix);
}
