package com.a9.arbor.grc.modules.privacy.repo;

import com.a9.arbor.grc.modules.privacy.domain.RopaProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RopaProjectRepository extends JpaRepository<RopaProject, Long> {
    List<RopaProject> findAllByOrderByUpdatedAtDesc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
