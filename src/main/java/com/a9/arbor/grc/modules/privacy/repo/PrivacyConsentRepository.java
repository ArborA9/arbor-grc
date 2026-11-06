package com.a9.arbor.grc.modules.privacy.repo;

import com.a9.arbor.grc.modules.privacy.domain.ConsentStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivacyConsentRepository extends JpaRepository<PrivacyConsent, Long> {

    List<PrivacyConsent> findAllByOrderByUpdatedAtDesc();

    long countByStatus(ConsentStatus status);

    long countByDisplayIdStartingWith(String prefix);
}
