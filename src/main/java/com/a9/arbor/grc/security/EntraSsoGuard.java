package com.a9.arbor.grc.security;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.modules.iam.directory.EntraGraphDirectoryClient;
import com.a9.arbor.grc.modules.iam.directory.EntraIssuer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSO sessions stay valid only while Entra still has the account enabled.
 * Graph down, missing permission, or no matching user keeps the current Arbor token
 * (fail open) so an IdP outage does not lock the workspace.
 */
@Service
public class EntraSsoGuard {

    private static final Logger log = LoggerFactory.getLogger(EntraSsoGuard.class);
    private static final Duration ALLOW_FOR = Duration.ofMinutes(5);
    private static final Duration DENY_FOR = Duration.ofMinutes(1);
    private static final Duration UNKNOWN_FOR = Duration.ofSeconds(30);

    private final TenantRepository tenantRepository;
    private final EntraGraphDirectoryClient graphClient;
    private final TransactionTemplate catalogTx;
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    public EntraSsoGuard(TenantRepository tenantRepository,
                         EntraGraphDirectoryClient graphClient,
                         @Qualifier("catalogTransactionManager") PlatformTransactionManager catalogTransactionManager) {
        this.tenantRepository = tenantRepository;
        this.graphClient = graphClient;
        this.catalogTx = new TransactionTemplate(catalogTransactionManager);
        this.catalogTx.setReadOnly(true);
    }

    /** @return false only when Entra says this account is disabled */
    public boolean allow(String tenantId, String email) {
        if (tenantId == null || tenantId.isBlank() || email == null || email.isBlank()) {
            return true;
        }
        String key = tenantId + "|" + email.trim().toLowerCase();
        Cached cached = cache.get(key);
        if (cached != null && Instant.now().isBefore(cached.until())) {
            return cached.allow();
        }
        Cached fresh = lookup(tenantId, email.trim());
        cache.put(key, fresh);
        return fresh.allow();
    }

    private Cached lookup(String tenantId, String email) {
        Tenant tenant = catalogTx.execute(status -> tenantRepository.findByTenantId(tenantId).orElse(null));
        if (tenant == null || !tenant.isSsoEnabled()) {
            return allowFor(ALLOW_FOR);
        }
        if (isBlank(tenant.getOidcIssuer()) || isBlank(tenant.getOidcClientId())
                || isBlank(tenant.getOidcClientSecret())) {
            return allowFor(ALLOW_FOR);
        }
        String entraId = EntraIssuer.directoryId(tenant.getOidcIssuer()).orElse(null);
        if (entraId == null) {
            return allowFor(ALLOW_FOR);
        }
        try {
            Optional<Boolean> enabled = graphClient.accountEnabled(
                    entraId, tenant.getOidcClientId(), tenant.getOidcClientSecret(), email);
            if (enabled.isEmpty()) {
                return allowFor(UNKNOWN_FOR);
            }
            if (!enabled.get()) {
                log.info("SSO session ended: Entra account disabled for tenant {}", tenantId);
                return new Cached(false, Instant.now().plus(DENY_FOR));
            }
            return allowFor(ALLOW_FOR);
        } catch (RuntimeException e) {
            log.warn("Entra account check unavailable for tenant {}: {}", tenantId, e.getMessage());
            return allowFor(UNKNOWN_FOR);
        }
    }

    private static Cached allowFor(Duration ttl) {
        return new Cached(true, Instant.now().plus(ttl));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Cached(boolean allow, Instant until) {
    }
}
