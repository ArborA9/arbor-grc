package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.domain.SuperAdmin;
import com.a9.arbor.grc.catalog.domain.SuperAdminReset;
import com.a9.arbor.grc.catalog.repo.SuperAdminRepository;
import com.a9.arbor.grc.catalog.repo.SuperAdminResetRepository;
import com.a9.arbor.grc.modules.iam.service.InvalidCredentialsException;
import com.a9.arbor.grc.security.PasswordPolicyService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/** Authentication and bootstrap for platform operators (Super Admins), stored in the catalog DB. */
@Service
public class SuperAdminService {

    /** Short codes are guessable if the app setting ever leaks into logs. */
    static final int MIN_RESET_CODE_LENGTH = 24;

    public enum ResetOutcome { APPLIED, ALREADY_USED }

    private final SuperAdminRepository repository;
    private final SuperAdminResetRepository resetRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;

    public SuperAdminService(SuperAdminRepository repository, SuperAdminResetRepository resetRepository,
                             PasswordEncoder passwordEncoder, PasswordPolicyService passwordPolicyService) {
        this.repository = repository;
        this.resetRepository = resetRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
    }

    public SuperAdmin authenticate(String email, String rawPassword) {
        return repository.findByEmail(email)
                .filter(sa -> "ACTIVE".equals(sa.getStatus()))
                .filter(sa -> passwordEncoder.matches(rawPassword, sa.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
    }

    /** Seeds the first Super Admin if none exists. Idempotent. */
    @Transactional(transactionManager = "catalogTransactionManager")
    public void seedIfEmpty(String email, String rawPassword, String fullName) {
        if (repository.count() > 0) {
            return;
        }
        SuperAdmin admin = new SuperAdmin();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        admin.setFullName(fullName);
        admin.setStatus("ACTIVE");
        repository.save(admin);
    }

    /**
     * Emergency reset from the deployment: sets the Super Admin password to {@code rawPassword},
     * re-activates the account, and requires a password change at next sign-in. Each code is
     * applied once; a restart with the same code leaves the account alone.
     */
    @Transactional(transactionManager = "catalogTransactionManager")
    public ResetOutcome emergencyReset(String email, String rawPassword, String fullName, String resetCode) {
        String code = resetCode == null ? "" : resetCode.trim();
        if (code.length() < MIN_RESET_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "Super Admin reset code must be at least " + MIN_RESET_CODE_LENGTH + " characters");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Super Admin email is not configured");
        }
        String codeHash = sha256(code);
        if (resetRepository.existsByCodeHash(codeHash)) {
            return ResetOutcome.ALREADY_USED;
        }
        passwordPolicyService.assertValid(rawPassword);

        SuperAdmin admin = repository.findByEmail(email).orElseGet(() -> {
            SuperAdmin created = new SuperAdmin();
            created.setEmail(email);
            created.setFullName(fullName == null || fullName.isBlank() ? "Arbor Platform Admin" : fullName);
            return created;
        });
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        admin.setStatus("ACTIVE");
        admin.setMustChangePassword(true);
        repository.save(admin);

        SuperAdminReset applied = new SuperAdminReset();
        applied.setCodeHash(codeHash);
        applied.setEmail(email);
        applied.setAppliedAt(Instant.now());
        resetRepository.save(applied);
        return ResetOutcome.APPLIED;
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public SuperAdmin changePassword(String email, String currentPassword, String newPassword) {
        SuperAdmin admin = authenticate(email, currentPassword);
        passwordPolicyService.assertValid(newPassword);
        if (currentPassword.equals(newPassword) || passwordEncoder.matches(newPassword, admin.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New password must be different from the current password");
        }
        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        admin.setMustChangePassword(false);
        return repository.save(admin);
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
