package com.a9.arbor.grc.modules.iam.service;

import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.common.validation.EmailRules;
import com.a9.arbor.grc.config.ArborNotificationProperties;
import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.modules.iam.domain.AuthType;
import com.a9.arbor.grc.modules.iam.domain.PasswordLinkPurpose;
import com.a9.arbor.grc.modules.iam.domain.PasswordReset;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.PasswordResetRepository;
import com.a9.arbor.grc.modules.iam.web.dto.ForgotPasswordResponse;
import com.a9.arbor.grc.modules.iam.web.dto.PasswordLinkStatusResponse;
import com.a9.arbor.grc.modules.iam.web.dto.ResetPasswordResponse;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.security.PasswordPolicyService;
import com.a9.arbor.grc.security.SecureTokenUtils;
import com.a9.arbor.grc.tenancy.TenantContext;
import com.a9.arbor.grc.tenancy.TenantDataSourceManager;
import com.a9.arbor.grc.tenancy.TenantIds;
import com.a9.arbor.grc.tenancy.UnknownTenantException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Set-password links for LOCAL accounts. Link only: no code is emailed.
 * Activation (new Owner) lasts 24 hours; reset (forgot password, local login turned on) lasts 1 hour.
 * SSO and other non-local emails return a kind so the sign-in page can explain why.
 */
@Service
public class PasswordResetService {

    static final long RESET_EXPIRY_SECONDS = 3600;
    static final long ACTIVATION_EXPIRY_SECONDS = 24 * 3600;
    private static final DateTimeFormatter EMAIL_INSTANT =
            DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a 'UAE time'", Locale.ENGLISH)
                    .withZone(ZoneId.of("Asia/Dubai"));

    private final UserService userService;
    private final PasswordResetRepository passwordResetRepository;
    private final NotificationService notificationService;
    private final ArborNotificationProperties notificationProperties;
    private final ArborSecurityProperties securityProperties;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final TenantRepository tenantRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final TransactionTemplate writeTx;

    public PasswordResetService(UserService userService,
                                PasswordResetRepository passwordResetRepository,
                                NotificationService notificationService,
                                ArborNotificationProperties notificationProperties,
                                ArborSecurityProperties securityProperties,
                                TenantDataSourceManager tenantDataSourceManager,
                                TenantRepository tenantRepository,
                                PasswordPolicyService passwordPolicyService,
                                @Qualifier("tenantTransactionManager") PlatformTransactionManager transactionManager) {
        this.userService = userService;
        this.passwordResetRepository = passwordResetRepository;
        this.notificationService = notificationService;
        this.notificationProperties = notificationProperties;
        this.securityProperties = securityProperties;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.tenantRepository = tenantRepository;
        this.passwordPolicyService = passwordPolicyService;
        this.writeTx = new TransactionTemplate(transactionManager);
    }

    public ForgotPasswordResponse requestReset(String tenantIdHeader, String emailRaw) {
        EmailRules.requireValid(emailRaw, "Email");
        String email = emailRaw.trim().toLowerCase(Locale.ROOT);
        String tenantId = requireTenantId(tenantIdHeader);
        try {
            Issued issued = withTenant(tenantId, () -> classifyAndIssue(tenantId, email));
            if ("LOCAL".equals(issued.kind())) {
                return ForgotPasswordResponse.local(notificationProperties.emailLive() ? null : issued.link());
            }
            if ("SSO".equals(issued.kind())) {
                return ForgotPasswordResponse.sso();
            }
            return ForgotPasswordResponse.notLocal();
        } catch (UnknownTenantException e) {
            return ForgotPasswordResponse.notLocal();
        }
    }

    /** Looks at a link without using it, so the page can show the right screen before any typing. */
    public PasswordLinkStatusResponse linkStatus(String tenantIdHeader, String token) {
        String tenantId = TenantIds.normalize(tenantIdHeader);
        if (tenantId == null || tenantId.isBlank() || token == null || token.isBlank()) {
            return invalid(tenantId);
        }
        try {
            return withTenant(tenantId, () -> {
                PasswordReset row = passwordResetRepository.findByLinkTokenHash(SecureTokenUtils.sha256(token.trim()))
                        .orElse(null);
                if (row == null) {
                    return invalid(tenantId);
                }
                User user = userService.requireUser(row.getUserId());
                if (user.getStatus() == UserStatus.DISABLED || !user.isLocalLoginEnabled()) {
                    return invalid(tenantId);
                }
                return new PasswordLinkStatusResponse(
                        stateOf(row, user),
                        row.getPurpose().name(),
                        maskEmail(user.getEmail()),
                        tenantId,
                        organisation(tenantId),
                        row.getExpiresAt(),
                        passwordPolicyService.minLength(),
                        passwordPolicyService.mixedCaseRequired(),
                        passwordPolicyService.specialCharRequired());
            });
        } catch (UnknownTenantException e) {
            return invalid(tenantId);
        }
    }

    public ResetPasswordResponse completeReset(String tenantIdHeader, String token, String newPassword) {
        String tenantId = requireTenantId(tenantIdHeader);
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Open the link from your email to set your password");
        }
        try {
            return withTenant(tenantId, () -> applyReset(tenantId, token, newPassword));
        } catch (UnknownTenantException e) {
            throw linkNoLongerValid();
        }
    }

    private Issued classifyAndIssue(String tenantId, String email) {
        User user = userService.findByEmail(email)
                .or(() -> userService.findByEmail(email.trim()))
                .orElse(null);
        if (user == null || user.getStatus() == UserStatus.DISABLED) {
            return new Issued("NOT_LOCAL", null);
        }
        if (!user.isLocalLoginEnabled()) {
            return new Issued(user.getAuthType() == AuthType.SSO ? "SSO" : "NOT_LOCAL", null);
        }
        if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.INVITED) {
            return new Issued("NOT_LOCAL", null);
        }
        return new Issued("LOCAL", issueResetLink(tenantId, user));
    }

    /** New account: 24-hour activation email. Replaces any open link for this user. */
    @Transactional
    public String issueActivationLink(String tenantId, User user) {
        return issue(tenantId, user, PasswordLinkPurpose.ACTIVATION);
    }

    /** Forgot password, or local login turned back on: 1-hour reset email. Replaces any open link. */
    @Transactional
    public String issueResetLink(String tenantId, User user) {
        return issue(tenantId, user, PasswordLinkPurpose.RESET);
    }

    private String issue(String tenantId, User user, PasswordLinkPurpose purpose) {
        Instant now = Instant.now();
        passwordResetRepository.invalidateOpenForUser(user.getId(), now);

        boolean activation = purpose == PasswordLinkPurpose.ACTIVATION;
        Instant expiresAt = now.plusSeconds(activation ? ACTIVATION_EXPIRY_SECONDS : RESET_EXPIRY_SECONDS);
        String rawToken = SecureTokenUtils.randomToken();
        PasswordReset row = new PasswordReset();
        row.setUserId(user.getId());
        row.setPurpose(purpose);
        // otp_hash is NOT NULL (V90). No code is sent, so store a hash nobody holds.
        row.setOtpHash(SecureTokenUtils.sha256(SecureTokenUtils.randomToken()));
        row.setLinkTokenHash(SecureTokenUtils.sha256(rawToken));
        row.setExpiresAt(expiresAt);
        passwordResetRepository.save(row);

        String link = link(tenantId, rawToken);
        notificationService.queueDirectEmail(user.getEmail(), activation ? "ACCOUNT_ACTIVATION" : "PASSWORD_RESET",
                Map.of(
                        "email", user.getEmail(),
                        "fullName", user.getFullName() == null ? "" : user.getFullName(),
                        "workspaceId", tenantId,
                        "actionLink", link,
                        "validUntil", EMAIL_INSTANT.format(expiresAt),
                        "validFor", activation ? "24 hours" : "1 hour"));
        return link;
    }

    private ResetPasswordResponse applyReset(String tenantId, String token, String newPassword) {
        PasswordReset row = passwordResetRepository.findByLinkTokenHash(SecureTokenUtils.sha256(token.trim()))
                .filter(PasswordReset::isActive)
                .orElseThrow(PasswordResetService::linkNoLongerValid);
        User user = userService.requireUser(row.getUserId());
        if (!user.isLocalLoginEnabled() || user.getStatus() == UserStatus.DISABLED) {
            throw linkNoLongerValid();
        }
        if (user.getStatus() == UserStatus.INVITED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userService.setLocalPassword(user, newPassword);
        Instant now = Instant.now();
        row.setUsedAt(now);
        passwordResetRepository.save(row);
        passwordResetRepository.invalidateOpenForUser(user.getId(), now);
        return new ResetPasswordResponse(row.getPurpose().name(), user.getEmail(), tenantId);
    }

    private static String stateOf(PasswordReset row, User user) {
        if (row.isActive()) {
            return "VALID";
        }
        if (row.getPurpose() == PasswordLinkPurpose.ACTIVATION && user.getPasswordHash() != null) {
            return "ACTIVATED";
        }
        return row.getUsedAt() != null ? "USED" : "EXPIRED";
    }

    private PasswordLinkStatusResponse invalid(String tenantId) {
        return new PasswordLinkStatusResponse("INVALID", null, null, tenantId, null, null,
                passwordPolicyService.minLength(), passwordPolicyService.mixedCaseRequired(),
                passwordPolicyService.specialCharRequired());
    }

    private String organisation(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .map(t -> t.getName() == null || t.getName().isBlank() ? tenantId : t.getName().trim())
                .orElse(tenantId);
    }

    static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private static ResponseStatusException linkNoLongerValid() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "This link has expired or was already used. Request a new one.");
    }

    private static String requireTenantId(String tenantIdHeader) {
        String tenantId = TenantIds.normalize(tenantIdHeader);
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace ID is required");
        }
        return tenantId;
    }

    private <T> T withTenant(String tenantId, Supplier<T> work) {
        tenantDataSourceManager.ensureRegistered(tenantId);
        TenantContext.setTenantId(tenantId);
        try {
            return writeTx.execute(status -> work.get());
        } finally {
            TenantContext.clear();
        }
    }

    private String link(String tenantId, String rawToken) {
        String base = notificationProperties.getAppBaseUrl();
        if (base == null || base.isBlank()) {
            base = securityProperties.getSso().getFrontendBaseUrl();
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/signin/reset?tenant=" + URLEncoder.encode(tenantId, StandardCharsets.UTF_8)
                + "&token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private record Issued(String kind, String link) {
    }
}
