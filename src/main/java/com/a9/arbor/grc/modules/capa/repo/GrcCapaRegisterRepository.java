package com.a9.arbor.grc.modules.capa.repo;

import com.a9.arbor.grc.modules.capa.domain.GrcCapaRegister;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrcCapaRegisterRepository extends JpaRepository<GrcCapaRegister, Long> {
    boolean existsByAuditId(Long auditId);

    Optional<GrcCapaRegister> findByAuditId(Long auditId);

    boolean existsByRegisterYear(Integer registerYear);

    Optional<GrcCapaRegister> findFirstByRegisterYearOrderByIdAsc(Integer registerYear);

    List<GrcCapaRegister> findAllByOrderByRegisterYearDescIdDesc();
}
