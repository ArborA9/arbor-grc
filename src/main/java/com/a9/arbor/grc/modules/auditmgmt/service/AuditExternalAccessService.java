package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.config.ArborAuditProperties;
import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditType;
import com.a9.arbor.grc.modules.auditmgmt.domain.ExternalAccessStatus;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditExternalAccess;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditExternalAccessRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditRepository;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.ExternalAccessInviteResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.ValidateExternalAccessResponse;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.ExternalAuditorUserService;
import com.a9.arbor.grc.modules.iam.web.dto.LoginResponse;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.security.JwtService;
import com.a9.arbor.grc.security.SecureTokenUtils;
import com.a9.arbor.grc.security.otp.OtpAttemptLimiter;
import com.a9.arbor.grc.tenancy.TenantContext;
import com.a9.arbor.grc.tenancy.TenantDataSourceManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AuditExternalAccessService {

    private static final DateTimeFormatter EXPIRY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final GrcAuditRepository auditRepository;
    private final GrcAuditExternalAccessRepository accessRepository;
    private final ExternalAuditorUserService externalAuditorUserService;
    private final NotificationService notificationService;
    private final JwtService jwtService;
    private final AuditTrailService auditTrailService;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final ArborAuditProperties auditProperties;
    private final ArborSecurityProperties securityProperties;
    private final OtpAttemptLimiter otpAttemptLimiter;
    private final TransactionTemplate readTx;
    private final TransactionTemplate writeTx;

    public AuditExternalAccessService(GrcAuditRepository auditRepository,
                                      GrcAuditExternalAccessRepository accessRepository,
                                      ExternalAuditorUserService externalAuditorUserService,
                                      NotificationService notificationService,
                                      JwtService jwtService,
                                      AuditTrailService auditTrailService,
                                      TenantDataSourceManager tenantDataSourceManager,
                                      ArborAuditProperties auditProperties,
                                      ArborSecurityProperties securityProperties,
                                      OtpAttemptLimiter otpAttemptLimiter,
                                      PlatformTransactionManager transactionManager) {
        this.auditRepository = auditRepository;
        this.accessRepository = accessRepository;
        this.externalAuditorUserService = externalAuditorUserService;
        this.notificationService = notificationService;
        this.jwtService = jwtService;
        this.auditTrailService = auditTrailService;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.auditProperties = auditProperties;
        this.securityProperties = securityProperties;
        this.otpAttemptLimiter = otpAttemptLimiter;
        this.readTx = new TransactionTemplate(transactionManager);
        this.readTx.setReadOnly(true);
        this.writeTx = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public ExternalAccessInviteResponse invite(User admin, long auditId, String name, String email,
                                               Integer expiryDaysOverride) {
        if (!isAdmin(admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        GrcAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit not found"));
        if (audit.getAuditType() != AuditType.EXTERNAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "External OTP invites apply to external audits only");
        }

        int expiryDays = expiryDaysOverride != null && expiryDaysOverride > 0
                ? expiryDaysOverride
                : auditProperties.getExternalAccess().getExpiryDays();
        Instant expiresAt = Instant.now().plus(expiryDays, ChronoUnit.DAYS);

        String rawToken = SecureTokenUtils.randomToken();
        String rawOtp = SecureTokenUtils.randomOtp();

        GrcAuditExternalAccess row = new GrcAuditExternalAccess();
        row.setAuditId(auditId);
        row.setInviteeName(name.trim());
        row.setInviteeEmail(email.trim().toLowerCase(Locale.ROOT));
        row.setTokenHash(SecureTokenUtils.sha256(rawToken));
        row.setOtpHash(SecureTokenUtils.sha256(rawOtp));
        row.setStatus(ExternalAccessStatus.PENDING);
        row.setExpiresAt(expiresAt);
        row.setCreatedByUserId(admin.getId());
        accessRepository.save(row);

        String tenantId = TenantContext.getTenantId();
        String accessLink = securityProperties.getSso().getFrontendBaseUrl()
                + "/external-audit?tenant=" + urlEnc(tenantId) + "&token=" + urlEnc(rawToken);

        Map<String, String> data = new HashMap<>();
        data.put("auditName", audit.getName());
        data.put("inviteeName", row.getInviteeName());
        data.put("accessLink", accessLink);
        data.put("otpCode", rawOtp);
        data.put("expiresAt", EXPIRY_FMT.format(expiresAt));

        notificationService.queueDirectEmail(row.getInviteeEmail(), "AUDIT_EXTERNAL_INVITE_LINK", data);
        notificationService.queueDirectEmail(row.getInviteeEmail(), "AUDIT_EXTERNAL_INVITE_OTP", data);

        auditTrailService.record(AuditActionType.CREATED, "AUDIT", admin.getId(),
                adminRoles(admin), "GRC_AUDIT_EXTERNAL_ACCESS", String.valueOf(row.getId()),
                audit.getName(), null, null, row.getInviteeEmail(), null, null);

        if (auditProperties.getExternalAccess().isExposeCredentialsInResponse()) {
            return ExternalAccessInviteResponse.from(row, accessLink, rawOtp, rawToken);
        }
        return ExternalAccessInviteResponse.from(row);
    }

    @Transactional(readOnly = true)
    public List<ExternalAccessInviteResponse> listInvites(User admin, long auditId) {
        if (!isAdmin(admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        auditRepository.findById(auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit not found"));
        return accessRepository.findByAuditIdOrderByCreatedAtDesc(auditId).stream()
                .map(ExternalAccessInviteResponse::from)
                .toList();
    }

    public ValidateExternalAccessResponse validate(String tenantId, String rawToken) {
        return withTenant(tenantId, readTx, () -> {
            GrcAuditExternalAccess row = findByRawToken(rawToken);
            expireIfNeeded(row);
            GrcAudit audit = auditRepository.findById(row.getAuditId()).orElse(null);
            if (row.getStatus() == ExternalAccessStatus.REVOKED) {
                return invalid(row, audit, "Invite revoked");
            }
            if (row.getStatus() == ExternalAccessStatus.EXPIRED || row.getExpiresAt().isBefore(Instant.now())) {
                return invalid(row, audit, "Invite expired");
            }
            return new ValidateExternalAccessResponse(
                    true, row.getStatus(),
                    audit != null ? audit.getName() : "-",
                    row.getInviteeName(), row.getInviteeEmail(),
                    row.getExpiresAt(), "OK");
        });
    }

    public LoginResponse redeem(String tenantId, String rawToken, String rawOtp) {
        String otpScope = otpScopeKey(tenantId, rawToken);
        otpAttemptLimiter.assertAllowed(otpScope);
        return withTenant(tenantId, writeTx, () -> {
            GrcAuditExternalAccess row = findByRawToken(rawToken);
            expireIfNeeded(row);
            if (row.getStatus() == ExternalAccessStatus.REVOKED) {
                throw new ResponseStatusException(HttpStatus.GONE, "Invite revoked");
            }
            if (row.getStatus() == ExternalAccessStatus.EXPIRED || row.getExpiresAt().isBefore(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.GONE, "Invite expired");
            }
            if (!SecureTokenUtils.sha256(rawOtp.trim()).equals(row.getOtpHash())) {
                otpAttemptLimiter.recordFailure(otpScope);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP");
            }
            otpAttemptLimiter.clear(otpScope);

            User user = resolveEphemeralUser(row);
            row.setUserId(user.getId());
            row.setStatus(ExternalAccessStatus.ACTIVE);
            row.setRedeemedAt(Instant.now());
            accessRepository.save(row);

            long minutesLeft = ChronoUnit.MINUTES.between(Instant.now(), row.getExpiresAt());
            if (minutesLeft < 1) {
                minutesLeft = 1;
            }
            List<String> roles = List.of(Role.EXTERNAL_AUDITOR.name());
            String token = jwtService.issueForUser(user, tenantId, roles, minutesLeft);

            auditTrailService.record(AuditActionType.LOGIN, "AUDIT", user.getId(),
                    Role.EXTERNAL_AUDITOR.name(), "GRC_AUDIT_EXTERNAL_ACCESS", String.valueOf(row.getId()),
                    String.valueOf(row.getAuditId()), null, null, "OTP redeem", null, null);

            return LoginResponse.full(token, user.getEmail(), tenantId, roles, minutesLeft);
        });
    }

    public boolean hasActiveAccess(Long userId, long auditId) {
        return accessRepository.existsByAuditIdAndUserIdAndStatus(
                auditId, userId, ExternalAccessStatus.ACTIVE);
    }

    private User resolveEphemeralUser(GrcAuditExternalAccess row) {
        return externalAuditorUserService.resolveOrCreate(row.getInviteeEmail(), row.getInviteeName());
    }

    private static String otpScopeKey(String tenantId, String rawToken) {
        String token = rawToken == null ? "" : rawToken.trim();
        return tenantId + ":audit:" + SecureTokenUtils.sha256(token);
    }

    private GrcAuditExternalAccess findByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token required");
        }
        return accessRepository.findByTokenHash(SecureTokenUtils.sha256(rawToken.trim()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token"));
    }

    private void expireIfNeeded(GrcAuditExternalAccess row) {
        if (row.getExpiresAt().isBefore(Instant.now())
                && row.getStatus() != ExternalAccessStatus.EXPIRED
                && row.getStatus() != ExternalAccessStatus.REVOKED) {
            row.setStatus(ExternalAccessStatus.EXPIRED);
            accessRepository.save(row);
        }
    }

    private ValidateExternalAccessResponse invalid(GrcAuditExternalAccess row, GrcAudit audit, String message) {
        return new ValidateExternalAccessResponse(
                false, row.getStatus(),
                audit != null ? audit.getName() : "-",
                row.getInviteeName(), row.getInviteeEmail(),
                row.getExpiresAt(), message);
    }

    private <T> T withTenant(String tenantId, TransactionTemplate tx, java.util.function.Supplier<T> work) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant required");
        }
        tenantDataSourceManager.ensureRegistered(tenantId);
        TenantContext.setTenantId(tenantId);
        try {
            return tx.execute(status -> work.get());
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN);
    }

    private static String adminRoles(User admin) {
        return admin.getRoles().stream().map(Enum::name).sorted().reduce((a, b) -> a + "," + b).orElse("");
    }

    private static String urlEnc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
