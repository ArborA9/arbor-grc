package com.a9.arbor.grc.modules.bcms.repo;

import com.a9.arbor.grc.modules.bcms.domain.BiaMor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BiaMorRepository extends JpaRepository<BiaMor, Long> {
    Optional<BiaMor> findByProcessId(Long processId);
    void deleteByProcessId(Long processId);
}
