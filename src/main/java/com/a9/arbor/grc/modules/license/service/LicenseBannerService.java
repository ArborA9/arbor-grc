package com.a9.arbor.grc.modules.license.service;

import com.a9.arbor.grc.modules.license.domain.LicenseBannerDismiss;
import com.a9.arbor.grc.modules.license.repo.LicenseBannerDismissRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LicenseBannerService {

    private final LicenseBannerDismissRepository repository;

    public LicenseBannerService(LicenseBannerDismissRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isDismissed(Long userId, String bannerKey) {
        if (userId == null || bannerKey == null || bannerKey.isBlank()) {
            return false;
        }
        return repository.existsByUserIdAndBannerKey(userId, bannerKey);
    }

    @Transactional
    public void dismiss(Long userId, String bannerKey) {
        if (userId == null || bannerKey == null || bannerKey.isBlank()) {
            return;
        }
        if (repository.existsByUserIdAndBannerKey(userId, bannerKey)) {
            return;
        }
        LicenseBannerDismiss row = new LicenseBannerDismiss();
        row.setUserId(userId);
        row.setBannerKey(bannerKey.trim());
        repository.save(row);
    }
}
