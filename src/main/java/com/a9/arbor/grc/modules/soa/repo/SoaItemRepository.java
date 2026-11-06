package com.a9.arbor.grc.modules.soa.repo;

import com.a9.arbor.grc.modules.soa.domain.SoaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SoaItemRepository extends JpaRepository<SoaItem, Long> {
    List<SoaItem> findBySoaIdOrderByNodeCodeAsc(Long soaId);

    Optional<SoaItem> findByIdAndSoaId(Long id, Long soaId);

    long countBySoaId(Long soaId);
}
