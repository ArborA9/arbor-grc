package com.a9.arbor.grc.modules.tprm.repo;

import com.a9.arbor.grc.modules.tprm.domain.TprmVendorToken;
import com.a9.arbor.grc.modules.tprm.domain.VendorTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TprmVendorTokenRepository extends JpaRepository<TprmVendorToken, Long> {
    Optional<TprmVendorToken> findByTokenHash(String tokenHash);

    List<TprmVendorToken> findByUserIdAndStatusOrderByIdDesc(Long userId, VendorTokenStatus status);

    List<TprmVendorToken> findByAssessmentId(Long assessmentId);
}
