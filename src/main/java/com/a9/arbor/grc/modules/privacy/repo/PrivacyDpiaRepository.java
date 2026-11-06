package com.a9.arbor.grc.modules.privacy.repo;

import com.a9.arbor.grc.modules.privacy.domain.DpiaStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyDpia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrivacyDpiaRepository extends JpaRepository<PrivacyDpia, Long> {

    List<PrivacyDpia> findAllByOrderByUpdatedAtDesc();

    Optional<PrivacyDpia> findFirstByRopaIdAndStatusOrderByUpdatedAtDesc(Long ropaId, DpiaStatus status);

    Optional<PrivacyDpia> findFirstByRopaIdOrderByUpdatedAtDesc(Long ropaId);

    java.util.List<PrivacyDpia> findByRopaId(Long ropaId);

    long countByDisplayIdStartingWith(String prefix);
}
