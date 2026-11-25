package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.domain.SuperAdmin;
import com.a9.arbor.grc.catalog.domain.SuperAdminMfaTotp;
import com.a9.arbor.grc.catalog.repo.SuperAdminMfaTotpRepository;
import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.modules.iam.web.dto.MfaEnrollStartResponse;
import com.a9.arbor.grc.modules.iam.web.dto.MfaStatusResponse;
import com.a9.arbor.grc.security.mfa.TotpSecretCipher;
import com.a9.arbor.grc.security.mfa.TotpService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class SuperAdminMfaService {

    private final SuperAdminMfaTotpRepository repository;
    private final TotpService totpService;
    private final TotpSecretCipher cipher;
    private final String issuer;

    public SuperAdminMfaService(SuperAdminMfaTotpRepository repository,
                                TotpService totpService,
                                TotpSecretCipher cipher,
                                ArborSecurityProperties properties) {
        this.repository = repository;
        this.totpService = totpService;
        this.cipher = cipher;
        this.issuer = properties.getMfa().getIssuer();
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public boolean isEnabledFor(SuperAdmin admin) {
        return repository.findBySuperAdminId(admin.getId())
                .filter(SuperAdminMfaTotp::isEnabled)
                .isPresent();
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public MfaStatusResponse status(SuperAdmin admin) {
        return repository.findBySuperAdminId(admin.getId())
                .map(r -> new MfaStatusResponse(true, r.isEnabled()))
                .orElse(new MfaStatusResponse(false, false));
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public MfaEnrollStartResponse beginEnroll(SuperAdmin admin) {
        String secret = totpService.generateSecret();
        SuperAdminMfaTotp row = repository.findBySuperAdminId(admin.getId()).orElseGet(SuperAdminMfaTotp::new);
        row.setSuperAdminId(admin.getId());
        row.setSecretEncrypted(cipher.encrypt(secret));
        row.setEnabled(false);
        row.setConfirmedAt(null);
        row.setLastUsedAt(null);
        repository.save(row);
        String uri = totpService.buildAuthUri(secret, admin.getEmail(), issuer);
        return new MfaEnrollStartResponse(secret, uri);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public MfaStatusResponse confirmEnroll(SuperAdmin admin, String code) {
        SuperAdminMfaTotp row = repository.findBySuperAdminId(admin.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start enrollment first"));
        String secret = cipher.decrypt(row.getSecretEncrypted());
        if (!totpService.verifyCode(secret, code.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authenticator code");
        }
        row.setEnabled(true);
        row.setConfirmedAt(Instant.now());
        row.setLastUsedAt(Instant.now());
        repository.save(row);
        return new MfaStatusResponse(true, true);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public MfaStatusResponse disable(SuperAdmin admin, String code) {
        SuperAdminMfaTotp row = repository.findBySuperAdminId(admin.getId())
                .filter(SuperAdminMfaTotp::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA is not enabled"));
        String secret = cipher.decrypt(row.getSecretEncrypted());
        if (!totpService.verifyCode(secret, code.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authenticator code");
        }
        repository.delete(row);
        return new MfaStatusResponse(false, false);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public boolean verifyEnabledCode(SuperAdmin admin, String code) {
        SuperAdminMfaTotp row = repository.findBySuperAdminId(admin.getId())
                .filter(SuperAdminMfaTotp::isEnabled)
                .orElse(null);
        if (row == null) {
            return false;
        }
        String secret = cipher.decrypt(row.getSecretEncrypted());
        if (!totpService.verifyCode(secret, code.trim())) {
            return false;
        }
        row.setLastUsedAt(Instant.now());
        repository.save(row);
        return true;
    }

    /** Break-glass: remove MFA when emergency password reset runs (Option 1). */
    @Transactional(transactionManager = "catalogTransactionManager")
    public void clearFor(Long superAdminId) {
        if (superAdminId == null) {
            return;
        }
        repository.deleteBySuperAdminId(superAdminId);
    }
}
