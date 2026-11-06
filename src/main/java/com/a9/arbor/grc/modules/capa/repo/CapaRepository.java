package com.a9.arbor.grc.modules.capa.repo;

import com.a9.arbor.grc.modules.capa.domain.Capa;
import com.a9.arbor.grc.modules.capa.domain.CapaRegister;
import com.a9.arbor.grc.modules.capa.domain.CapaSourceType;
import com.a9.arbor.grc.modules.capa.domain.CapaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CapaRepository extends JpaRepository<Capa, Long> {
    List<Capa> findAllByOrderByUpdatedAtDesc();
    List<Capa> findByRegisterCodeOrderByUpdatedAtDesc(CapaRegister registerCode);

    Optional<Capa> findFirstBySourceTypeAndSourceRecordIdAndStatusNot(
            CapaSourceType sourceType, Long sourceRecordId, CapaStatus status);

    boolean existsByAuditIdAndTitleIgnoreCase(Long auditId, String title);

    List<Capa> findByCapaRegisterIdOrderByIdAsc(Long capaRegisterId);

    long countByCapaRegisterId(Long capaRegisterId);

    boolean existsByCapaRegisterIdAndSourceRecordId(Long capaRegisterId, Long sourceRecordId);

    List<Capa> findByExternalFindingCodeIgnoreCase(String externalFindingCode);

    @Query("""
            select c from Capa c
            where c.targetDate = :dueSoonDate
              and c.status <> com.a9.arbor.grc.modules.capa.domain.CapaStatus.CLOSED
              and c.dueSoonNotifiedAt is null
            """)
    List<Capa> findDueSoonNotNotified(@Param("dueSoonDate") LocalDate dueSoonDate);

    @Query("""
            select c from Capa c
            where c.targetDate is not null
              and c.targetDate <= :asOf
              and c.status <> com.a9.arbor.grc.modules.capa.domain.CapaStatus.CLOSED
              and c.overdueNotifiedAt is null
            """)
    List<Capa> findPastTargetNotOverdueNotified(@Param("asOf") LocalDate asOf);

    @Query("""
            select c from Capa c
            where c.targetDate is not null
              and c.targetDate <= :escalationCutoff
              and c.status <> com.a9.arbor.grc.modules.capa.domain.CapaStatus.CLOSED
              and c.overdueNotifiedAt is not null
              and c.escalationNotifiedAt is null
            """)
    List<Capa> findEscalationDue(@Param("escalationCutoff") LocalDate escalationCutoff);
}
