package com.a9.arbor.grc.modules.capa.repo;

import com.a9.arbor.grc.modules.capa.domain.CapaReturnEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CapaReturnEventRepository extends JpaRepository<CapaReturnEvent, Long> {
    List<CapaReturnEvent> findByCapaIdOrderByRejectedAtDescIdDesc(long capaId);

    List<CapaReturnEvent> findByCapaIdInOrderByRejectedAtDescIdDesc(Collection<Long> capaIds);
}
