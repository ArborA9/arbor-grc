package com.a9.arbor.grc.modules.iam.service;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.modules.iam.directory.EntraGraphDirectoryClient;
import com.a9.arbor.grc.modules.iam.directory.EntraIssuer;
import com.a9.arbor.grc.modules.iam.directory.GraphDirectoryException;
import com.a9.arbor.grc.modules.iam.web.dto.EmployeeDirectoryEntry;
import com.a9.arbor.grc.modules.iam.web.dto.EmployeeDirectorySearchResponse;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Optional Entra employee lookup for SSO tenants. Local / non-SSO / Graph-down never throws
 * to the client except a short-query 400. Login and {@code /api/users/directory} are unchanged.
 */
@Service
public class EmployeeDirectoryService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeDirectoryService.class);

    private final TenantRepository tenantRepository;
    private final EntraGraphDirectoryClient graphClient;

    public EmployeeDirectoryService(TenantRepository tenantRepository,
                                    EntraGraphDirectoryClient graphClient) {
        this.tenantRepository = tenantRepository;
        this.graphClient = graphClient;
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public EmployeeDirectorySearchResponse search(String query) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return EmployeeDirectorySearchResponse.unavailable("no_tenant");
        }
        Tenant tenant = tenantRepository.findByTenantId(tenantId).orElse(null);
        if (tenant == null || !tenant.isSsoEnabled()) {
            return EmployeeDirectorySearchResponse.unavailable("sso_disabled");
        }
        if (isBlank(tenant.getOidcIssuer()) || isBlank(tenant.getOidcClientId())
                || isBlank(tenant.getOidcClientSecret())) {
            return EmployeeDirectorySearchResponse.unavailable("not_configured");
        }
        String entraId = EntraIssuer.directoryId(tenant.getOidcIssuer()).orElse(null);
        if (entraId == null) {
            return EmployeeDirectorySearchResponse.unavailable("not_entra");
        }
        String q = EntraIssuer.sanitizeQuery(query);
        if (q.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q must be at least 2 characters");
        }
        try {
            List<EmployeeDirectoryEntry> results = graphClient.search(
                    entraId, tenant.getOidcClientId(), tenant.getOidcClientSecret(), q);
            return EmployeeDirectorySearchResponse.ok(results);
        } catch (GraphDirectoryException e) {
            log.warn("Entra Graph directory search unavailable for tenant {}: {}", tenantId, e.getMessage());
            return EmployeeDirectorySearchResponse.unavailable("graph_unavailable");
        } catch (RuntimeException e) {
            log.warn("Entra Graph directory search failed for tenant {}: {}", tenantId, e.getMessage());
            return EmployeeDirectorySearchResponse.unavailable("graph_unavailable");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
