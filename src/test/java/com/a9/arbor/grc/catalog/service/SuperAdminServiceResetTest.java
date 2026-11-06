package com.a9.arbor.grc.catalog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.a9.arbor.grc.catalog.domain.SuperAdmin;
import com.a9.arbor.grc.catalog.domain.SuperAdminReset;
import com.a9.arbor.grc.catalog.repo.SuperAdminRepository;
import com.a9.arbor.grc.catalog.repo.SuperAdminResetRepository;
import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.security.PasswordPolicyService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class SuperAdminServiceResetTest {

    private static final String EMAIL = "superadmin@arborgrc.com";
    private static final String CODE = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4";
    private static final String TEMP = "Temp-Pass#2026x";

    private SuperAdminRepository repository;
    private SuperAdminResetRepository resetRepository;
    private PasswordEncoder encoder;
    private SuperAdminService service;

    @BeforeEach
    void setUp() {
        repository = mock(SuperAdminRepository.class);
        resetRepository = mock(SuperAdminResetRepository.class);
        encoder = new BCryptPasswordEncoder(4);
        ArborSecurityProperties props = new ArborSecurityProperties();
        props.getPassword().setStrict(true);
        service = new SuperAdminService(repository, resetRepository, encoder, new PasswordPolicyService(props));
        when(repository.save(any(SuperAdmin.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private SuperAdmin existing(String status) {
        SuperAdmin admin = new SuperAdmin();
        admin.setEmail(EMAIL);
        admin.setFullName("Arbor Platform Admin");
        admin.setPasswordHash(encoder.encode("Old-Pass#2026x"));
        admin.setStatus(status);
        return admin;
    }

    @Test
    void newCodeResetsPasswordReactivatesAndForcesChange() {
        SuperAdmin admin = existing("DISABLED");
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(admin));
        when(resetRepository.existsByCodeHash(SuperAdminService.sha256(CODE))).thenReturn(false);

        assertEquals(SuperAdminService.ResetOutcome.APPLIED, service.emergencyReset(EMAIL, TEMP, null, CODE));

        assertTrue(encoder.matches(TEMP, admin.getPasswordHash()));
        assertEquals("ACTIVE", admin.getStatus());
        assertTrue(admin.isMustChangePassword());
        ArgumentCaptor<SuperAdminReset> ledger = ArgumentCaptor.forClass(SuperAdminReset.class);
        verify(resetRepository).save(ledger.capture());
        assertEquals(SuperAdminService.sha256(CODE), ledger.getValue().getCodeHash());
        assertFalse(ledger.getValue().getCodeHash().contains(CODE));
    }

    @Test
    void usedCodeLeavesAccountAlone() {
        when(resetRepository.existsByCodeHash(SuperAdminService.sha256(CODE))).thenReturn(true);

        assertEquals(SuperAdminService.ResetOutcome.ALREADY_USED, service.emergencyReset(EMAIL, TEMP, null, CODE));

        verify(repository, never()).save(any());
        verify(resetRepository, never()).save(any());
    }

    @Test
    void shortCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.emergencyReset(EMAIL, TEMP, null, "short-code"));
        verify(repository, never()).save(any());
    }

    @Test
    void weakPasswordIsRejectedAndCodeNotConsumed() {
        when(resetRepository.existsByCodeHash(any())).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> service.emergencyReset(EMAIL, "weakpass", null, CODE));

        verify(repository, never()).save(any());
        verify(resetRepository, never()).save(any());
    }

    @Test
    void missingAccountIsCreated() {
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(resetRepository.existsByCodeHash(any())).thenReturn(false);

        service.emergencyReset(EMAIL, TEMP, "Arbor Platform Admin", CODE);

        ArgumentCaptor<SuperAdmin> saved = ArgumentCaptor.forClass(SuperAdmin.class);
        verify(repository).save(saved.capture());
        assertEquals(EMAIL, saved.getValue().getEmail());
        assertTrue(saved.getValue().isMustChangePassword());
    }

    @Test
    void changePasswordClearsForcedChange() {
        SuperAdmin admin = existing("ACTIVE");
        admin.setPasswordHash(encoder.encode(TEMP));
        admin.setMustChangePassword(true);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(admin));

        service.changePassword(EMAIL, TEMP, "New-Pass#2026y");

        assertFalse(admin.isMustChangePassword());
        assertTrue(encoder.matches("New-Pass#2026y", admin.getPasswordHash()));
    }
}
