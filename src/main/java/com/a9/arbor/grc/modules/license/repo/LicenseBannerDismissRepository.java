package com.a9.arbor.grc.modules.license.repo;

import com.a9.arbor.grc.modules.license.domain.LicenseBannerDismiss;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LicenseBannerDismissRepository extends JpaRepository<LicenseBannerDismiss, Long> {

    boolean existsByUserIdAndBannerKey(Long userId, String bannerKey);

    Optional<LicenseBannerDismiss> findByUserIdAndBannerKey(Long userId, String bannerKey);
}
