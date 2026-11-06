package com.a9.arbor.grc.modules.iam.repo;

import com.a9.arbor.grc.modules.iam.domain.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    Optional<PasswordReset> findByLinkTokenHash(String linkTokenHash);

    List<PasswordReset> findByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PasswordReset p set p.usedAt = :now where p.userId = :userId and p.usedAt is null")
    int invalidateOpenForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
