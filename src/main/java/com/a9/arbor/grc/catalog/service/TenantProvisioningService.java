package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.domain.TenantStatus;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.common.validation.EmailRules;
import com.a9.arbor.grc.config.ArborNotificationProperties;
import com.a9.arbor.grc.config.ArborTenancyProperties;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.service.PasswordResetService;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.tenancy.SqlServerDatabaseCreator;
import com.a9.arbor.grc.tenancy.TenantContext;
import com.a9.arbor.grc.tenancy.TenantDataSourceManager;
import com.a9.arbor.grc.tenancy.TenantIds;
import com.a9.arbor.grc.catalog.web.dto.CreateTenantRequest;
import com.a9.arbor.grc.catalog.web.dto.TenantIdpConfigRequest;
import com.a9.arbor.grc.security.oidc.TenantClientRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.SecureRandom;
import java.util.List;

/**
 * Provisions and lists tenants. Provisioning: create the catalog record, create the physical
 * database (SQL Server), migrate its schema, register its datasource, seed an initial admin user,
 * then mark it active.
 */
@Service
public class TenantProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(TenantProvisioningService.class);
    private static final String PW_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TenantRepository tenantRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final UserService userService;
    private final ArborTenancyProperties properties;
    private final TenantClientRegistrationRepository clientRegistrationRepository;
    private final ModuleLicensingService moduleLicensingService;
    private final PasswordResetService passwordResetService;
    private final ArborNotificationProperties notificationProperties;

    public TenantProvisioningService(TenantRepository tenantRepository,
                                     TenantDataSourceManager tenantDataSourceManager,
                                     UserService userService,
                                     ArborTenancyProperties properties,
                                     TenantClientRegistrationRepository clientRegistrationRepository,
                                     ModuleLicensingService moduleLicensingService,
                                     PasswordResetService passwordResetService,
                                     ArborNotificationProperties notificationProperties) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.userService = userService;
        this.properties = properties;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.moduleLicensingService = moduleLicensingService;
        this.passwordResetService = passwordResetService;
        this.notificationProperties = notificationProperties;
    }

    public List<Tenant> listTenants() {
        return tenantRepository.findAll();
    }

    /**
     * Configures (or disables) a tenant's SSO / AD (OIDC) integration. When enabling, the issuer and
     * client id are mandatory. A blank secret on update keeps the existing stored secret.
     */
    public Tenant configureIdp(String tenantId, TenantIdpConfigRequest request) {
        String id = TenantIds.normalize(tenantId);
        Tenant tenant = tenantRepository.findByTenantId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Unknown tenant: " + id));

        if (request.ssoEnabled()) {
            if (isBlank(request.oidcIssuer()) || isBlank(request.oidcClientId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "oidcIssuer and oidcClientId are required when SSO is enabled");
            }
        }

        tenant.setSsoEnabled(request.ssoEnabled());
        tenant.setOidcIssuer(emptyToNull(request.oidcIssuer()));
        tenant.setOidcClientId(emptyToNull(request.oidcClientId()));
        // Keep the existing secret when the caller leaves it blank (so the UI need not re-send it).
        if (!isBlank(request.oidcClientSecret())) {
            tenant.setOidcClientSecret(request.oidcClientSecret());
        }
        if (request.jitEnabled() != null) {
            tenant.setJitEnabled(request.jitEnabled());
        }
        if (request.jitDefaultRole() != null) {
            tenant.setJitDefaultRole(request.jitDefaultRole());
        }
        tenant = tenantRepository.save(tenant);
        clientRegistrationRepository.evict(id);
        log.info("Configured SSO for tenant {} (enabled={}, jit={}/{})", id, request.ssoEnabled(),
                tenant.isJitEnabled(), tenant.getJitDefaultRole());
        return tenant;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String emptyToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    public ProvisionResult provision(CreateTenantRequest request) {
        String tenantId = TenantIds.normalize(request.tenantId());
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId is required");
        }
        if (tenantRepository.existsByTenantId(tenantId)) {
            throw new TenantAlreadyExistsException(tenantId);
        }

        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setName(request.name());
        tenant.setStatus(TenantStatus.PROVISIONING);
        tenant.setJdbcUrl(properties.tenantJdbcUrl(tenantId));
        tenant.setDbUsername(properties.getUsername());
        tenant.setDbPassword(properties.getPassword());
        tenant = tenantRepository.save(tenant);

        if (properties.isCreateDatabase()) {
            SqlServerDatabaseCreator.createIfNotExists(
                    properties.getMasterUrl(),
                    properties.getUsername(),
                    properties.getPassword(),
                    properties.tenantDatabaseName(tenantId),
                    properties.getElasticPoolName(),
                    properties.getBackupStorageRedundancy());
        }

        boolean ssoNow = Boolean.TRUE.equals(request.ssoEnabled());
        boolean mailLive = notificationProperties.emailLive();
        String ownerEmail = emptyToNull(request.ownerEmail());
        String ownerName = emptyToNull(request.ownerFullName());
        if (ssoNow || mailLive) {
            if (ownerEmail == null || ownerName == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Owner email and full name are required");
            }
            EmailRules.requireValid(ownerEmail, "Owner email");
        } else {
            if (ownerEmail == null) {
                ownerEmail = "admin." + tenantId + "@arborgrc.com";
            } else {
                EmailRules.requireValid(ownerEmail, "Owner email");
            }
            if (ownerName == null) {
                ownerName = "Tenant Owner";
            }
        }
        if (ssoNow) {
            if (isBlank(request.oidcIssuer()) || isBlank(request.oidcClientId()) || isBlank(request.oidcClientSecret())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "oidcIssuer, oidcClientId, and oidcClientSecret are required when SSO is enabled");
            }
            tenant.setSsoEnabled(true);
            tenant.setOidcIssuer(request.oidcIssuer().trim());
            tenant.setOidcClientId(request.oidcClientId().trim());
            tenant.setOidcClientSecret(request.oidcClientSecret());
            tenant.setJitEnabled(false);
            tenant = tenantRepository.save(tenant);
        }

        tenantDataSourceManager.register(tenant);

        String adminPassword = null;
        boolean passwordEmailed = false;
        TenantContext.setTenantId(tenantId);
        try {
            if (ssoNow) {
                userService.createProvisionOwner(ownerEmail, ownerName, null, true);
            } else if (mailLive) {
                User owner = userService.createProvisionOwner(ownerEmail, ownerName, null, false);
                passwordResetService.issueActivationLink(tenantId, owner);
                passwordEmailed = true;
            } else {
                adminPassword = randomPassword(12);
                userService.createProvisionOwner(ownerEmail, ownerName, adminPassword, false);
            }
        } finally {
            TenantContext.clear();
        }

        tenant.setStatus(TenantStatus.ACTIVE);
        tenant = tenantRepository.save(tenant);
        if (ssoNow) {
            clientRegistrationRepository.evict(tenantId);
        }
        // Commercial default: every module disabled until Super Admin enables what was sold.
        moduleLicensingService.ensureDefaults(tenant.getTenantId());
        log.info("Provisioned tenant {} with owner {} (sso={}, passwordEmailed={})",
                tenant.getTenantId(), ownerEmail, ssoNow, passwordEmailed);
        return new ProvisionResult(tenant, ownerEmail, adminPassword, passwordEmailed, ssoNow);
    }

    /**
     * Sends a fresh 24-hour activation email to every Owner who has not set a password yet.
     * Older activation links for those Owners stop working.
     */
    public List<String> resendOwnerActivation(String rawTenantId) {
        String tenantId = TenantIds.normalize(rawTenantId);
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown tenant: " + tenantId));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant " + tenantId + " is not active");
        }
        tenantDataSourceManager.ensureRegistered(tenantId);
        TenantContext.setTenantId(tenantId);
        try {
            List<User> owners = userService.listUsers().stream()
                    .filter(u -> u.getRoles().contains(Role.OWNER))
                    .filter(u -> u.getStatus() != UserStatus.DISABLED)
                    .toList();
            List<User> pending = owners.stream()
                    .filter(User::isLocalLoginEnabled)
                    .filter(u -> u.getPasswordHash() == null)
                    .toList();
            if (pending.isEmpty()) {
                boolean activated = owners.stream()
                        .anyMatch(u -> u.isLocalLoginEnabled() && u.getPasswordHash() != null);
                throw new ResponseStatusException(HttpStatus.CONFLICT, activated
                        ? "The Owner has already activated this workspace. They can use Forgot password on the sign-in page."
                        : "No Owner is waiting for activation. SSO Owners sign in with company SSO.");
            }
            for (User owner : pending) {
                passwordResetService.issueActivationLink(tenantId, owner);
            }
            log.info("Resent owner activation for tenant {} to {} owner(s)", tenantId, pending.size());
            return pending.stream().map(User::getEmail).toList();
        } finally {
            TenantContext.clear();
        }
    }

    private static String randomPassword(int length) {
        char[] pw = new char[length];
        for (int i = 0; i < length; i++) {
            pw[i] = PW_ALPHABET.charAt(RANDOM.nextInt(PW_ALPHABET.length()));
        }
        String[] required = {"ABCDEFGHJKLMNPQRSTUVWXYZ", "abcdefghijkmnopqrstuvwxyz", "23456789", "!@#$%*-_"};
        for (int i = 0; i < required.length; i++) {
            pw[i] = required[i].charAt(RANDOM.nextInt(required[i].length()));
        }
        for (int i = length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char t = pw[i];
            pw[i] = pw[j];
            pw[j] = t;
        }
        return new String(pw);
    }

    public record ProvisionResult(Tenant tenant, String adminEmail, String adminPassword,
                                  boolean passwordEmailed, boolean ssoConfigured) {
    }
}
