package com.a9.arbor.grc.modules.kri.repo;

import com.a9.arbor.grc.modules.kri.domain.Kri;
import com.a9.arbor.grc.modules.kri.domain.KriStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KriRepository extends JpaRepository<Kri, Long> {
    List<Kri> findByStatusOrderByUpdatedAtDesc(KriStatus status);

    List<Kri> findAllByOrderByUpdatedAtDesc();

    List<Kri> findByOwnerUserIdAndStatusOrderByUpdatedAtDesc(Long ownerUserId, KriStatus status);

    Optional<Kri> findByDisplayIdIgnoreCase(String displayId);
}
