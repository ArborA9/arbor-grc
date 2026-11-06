package com.a9.arbor.grc.modules.org.service;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.modules.iam.directory.EntraGraphDirectoryClient;
import com.a9.arbor.grc.modules.iam.directory.EntraIssuer;
import com.a9.arbor.grc.modules.iam.directory.GraphDirectoryException;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.org.web.dto.DepartmentSyncResponse;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/** Copies distinct Entra user.department values into level-1 organisation units. */
@Service
public class DepartmentSyncService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentSyncService.class);

    private final TenantRepository tenantRepository;
    private final EntraGraphDirectoryClient graphClient;
    private final OrgStructureService orgStructureService;

    public DepartmentSyncService(TenantRepository tenantRepository,
                                 EntraGraphDirectoryClient graphClient,
                                 OrgStructureService orgStructureService) {
        this.tenantRepository = tenantRepository;
        this.graphClient = graphClient;
        this.orgStructureService = orgStructureService;
    }

    public DepartmentSyncResponse sync(User actor) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return DepartmentSyncResponse.unavailable("no_tenant");
        }
        Tenant tenant = tenantRepository.findByTenantId(tenantId).orElse(null);
        if (tenant == null || !tenant.isSsoEnabled()) {
            return DepartmentSyncResponse.unavailable("sso_disabled");
        }
        if (isBlank(tenant.getOidcIssuer()) || isBlank(tenant.getOidcClientId())
                || isBlank(tenant.getOidcClientSecret())) {
            return DepartmentSyncResponse.unavailable("not_configured");
        }
        String entraId = EntraIssuer.directoryId(tenant.getOidcIssuer()).orElse(null);
        if (entraId == null) {
            return DepartmentSyncResponse.unavailable("not_entra");
        }
        List<String> names;
        try {
            names = graphClient.listDepartments(entraId, tenant.getOidcClientId(), tenant.getOidcClientSecret());
        } catch (GraphDirectoryException e) {
            log.warn("Entra department sync unavailable for tenant {}: {}", tenantId, e.getMessage());
            return DepartmentSyncResponse.unavailable("graph_unavailable");
        } catch (RuntimeException e) {
            log.warn("Entra department sync failed for tenant {}: {}", tenantId, e.getMessage());
            return DepartmentSyncResponse.unavailable("graph_unavailable");
        }
        int added = orgStructureService.importEntraDepartments(actor, names);
        return DepartmentSyncResponse.ok(added, names);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
