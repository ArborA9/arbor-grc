package com.a9.arbor.grc.modules.soa.repo;

import com.a9.arbor.grc.modules.soa.domain.Soa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SoaRepository extends JpaRepository<Soa, Long> {
    List<Soa> findAllByOrderByUpdatedAtDesc();

    List<Soa> findByProjectIdOrderByUpdatedAtDesc(Long projectId);

    Optional<Soa> findByProjectIdAndFrameworkId(Long projectId, Long frameworkId);

    boolean existsByProjectIdAndFrameworkId(Long projectId, Long frameworkId);

    List<Soa> findByFrameworkIdOrderByUpdatedAtDesc(Long frameworkId);

    long countByProjectId(Long projectId);
}
