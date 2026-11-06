package com.a9.arbor.grc.modules.iam.service;

import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.modules.iam.domain.AuthType;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserMfaTotp;
import com.a9.arbor.grc.modules.iam.repo.UserMfaTotpRepository;
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
public class MfaService {

    private final UserMfaTotpRepository repository;
    private final TotpService totpService;
    private final TotpSecretCipher cipher;
    private final String issuer;

    public MfaService(UserMfaTotpRepository repository,
                      TotpService totpService,
                      TotpSecretCipher cipher,
                      ArborSecurityProperties properties) {
        this.repository = repository;
        this.totpService = totpService;
        this.cipher = cipher;
        this.issuer = properties.getMfa().getIssuer();
    }

    public static boolean isMfaEligible(User user) {
        boolean localDoor = user.isLocalLoginEnabled() || user.getAuthType() == AuthType.LOCAL;
        if (!localDoor) {
            return false;
        }
        return user.getRoles().contains(Role.OWNER) || user.getRoles().contains(Role.ADMIN);
    }

    @Transactional(readOnly = true)
    public boolean isEnabledFor(User user) {
        return repository.findByUserId(user.getId())
                .filter(UserMfaTotp::isEnabled)
                .isPresent();
    }

    @Transactional(readOnly = true)
    public MfaStatusResponse status(User user) {
        return repository.findByUserId(user.getId())
                .map(r -> new MfaStatusResponse(true, r.isEnabled()))
                .orElse(new MfaStatusResponse(false, false));
    }

    @Transactional
    public MfaEnrollStartResponse beginEnroll(User user) {
        assertEligible(user);
        String secret = totpService.generateSecret();
        UserMfaTotp row = repository.findByUserId(user.getId()).orElseGet(UserMfaTotp::new);
        row.setUserId(user.getId());
        row.setSecretEncrypted(cipher.encrypt(secret));
        row.setEnabled(false);
        row.setConfirmedAt(null);
        row.setLastUsedAt(null);
        repository.save(row);
        String uri = totpService.buildAuthUri(secret, user.getEmail(), issuer);
        return new MfaEnrollStartResponse(secret, uri);
    }

    @Transactional
    public MfaStatusResponse confirmEnroll(User user, String code) {
        assertEligible(user);
        UserMfaTotp row = repository.findByUserId(user.getId())
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

    @Transactional
    public MfaStatusResponse disable(User user, String code) {
        assertEligible(user);
        UserMfaTotp row = repository.findByUserId(user.getId())
                .filter(UserMfaTotp::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA is not enabled"));
        String secret = cipher.decrypt(row.getSecretEncrypted());
        if (!totpService.verifyCode(secret, code.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authenticator code");
        }
        repository.delete(row);
        return new MfaStatusResponse(false, false);
    }

    @Transactional
    public boolean verifyEnabledCode(User user, String code) {
        UserMfaTotp row = repository.findByUserId(user.getId())
                .filter(UserMfaTotp::isEnabled)
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

    private static void assertEligible(User user) {
        if (!isMfaEligible(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Local TOTP MFA is only for Owner/Admin local accounts");
        }
    }
}
