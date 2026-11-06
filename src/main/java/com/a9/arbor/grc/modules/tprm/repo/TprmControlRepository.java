package com.a9.arbor.grc.modules.tprm.repo;

import com.a9.arbor.grc.modules.tprm.domain.TprmControl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TprmControlRepository extends JpaRepository<TprmControl, Long> {
    List<TprmControl> findAllByOrderBySortOrderAsc();

    Optional<TprmControl> findByControlCode(String controlCode);

    Optional<TprmControl> findByControlCodeIgnoreCase(String controlCode);
}
