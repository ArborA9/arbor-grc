package com.a9.arbor.grc.modules.auditmgmt.repo;

import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementTeamRole;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GrcAuditTeamMemberRepository extends JpaRepository<GrcAuditTeamMember, GrcAuditTeamMember.Pk> {

    List<GrcAuditTeamMember> findByAuditIdOrderByTeamRoleAscUserIdAsc(Long auditId);

    void deleteByAuditId(Long auditId);

    @Query("select t.auditId from GrcAuditTeamMember t where t.userId = :userId")
    List<Long> findAuditIdsForUser(@Param("userId") Long userId);

    Optional<GrcAuditTeamMember> findByAuditIdAndTeamRole(Long auditId, EngagementTeamRole role);

    boolean existsByAuditIdAndUserId(Long auditId, Long userId);
}
