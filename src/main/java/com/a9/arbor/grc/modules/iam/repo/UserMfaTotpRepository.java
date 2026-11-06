package com.a9.arbor.grc.modules.iam.repo;

import com.a9.arbor.grc.modules.iam.domain.UserMfaTotp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserMfaTotpRepository extends JpaRepository<UserMfaTotp, Long> {
    Optional<UserMfaTotp> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
