package com.a9.arbor.grc.modules.iam.service;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.SsoGroupRoleMapping;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.SsoGroupRoleMappingRepository;
import com.a9.arbor.grc.modules.iam.web.dto.CreateSsoGroupRoleMappingRequest;
import com.a9.arbor.grc.modules.iam.web.dto.SsoGroupRoleMappingResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * IdP group → Arbor role mapping (Path C B2). Tenant-scoped: group names/object-ids come from
 * that tenant's own directory (Entra), so the mapping table lives in the tenant DB.
 */
@Service
public class SsoGroupRoleMappingService {

    private final SsoGroupRoleMappingRepository repository;
    private final AuditTrailService auditTrailService;

    public SsoGroupRoleMappingService(SsoGroupRoleMappingRepository repository,
                                      AuditTrailService auditTrailService) {
        this.repository = repository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<SsoGroupRoleMappingResponse> list() {
        return repository.findAllByOrderByIdpGroupNameAsc().stream()
                .map(SsoGroupRoleMappingResponse::from)
                .toList();
    }

    @Transactional
    public SsoGroupRoleMappingResponse create(User actor, CreateSsoGroupRoleMappingRequest request) {
        String groupName = request.idpGroupName().trim();
        if (groupName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idpGroupName is required");
        }
        if (!isMappableRole(request.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role cannot be assigned via SSO group mapping: " + request.role());
        }
        boolean duplicate = repository.findAllByOrderByIdpGroupNameAsc().stream()
                .anyMatch(m -> m.getIdpGroupName().equalsIgnoreCase(groupName) && m.getRole() == request.role());
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mapping already exists for group " + groupName + " -> " + request.role());
        }
        SsoGroupRoleMapping mapping = new SsoGroupRoleMapping();
        mapping.setIdpGroupName(groupName);
        mapping.setRole(request.role());
        mapping = repository.save(mapping);
        auditTrailService.record(AuditActionType.CREATED, "IAM", actor.getId(), primaryRole(actor),
                "SsoGroupRoleMapping", String.valueOf(mapping.getId()), groupName,
                "role", null, request.role().name(), null, null);
        return SsoGroupRoleMappingResponse.from(mapping);
    }

    @Transactional
    public void delete(User actor, long id) {
        SsoGroupRoleMapping mapping = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mapping not found"));
        repository.delete(mapping);
        auditTrailService.record(AuditActionType.DELETED, "IAM", actor.getId(), primaryRole(actor),
                "SsoGroupRoleMapping", String.valueOf(id), mapping.getIdpGroupName(),
                "role", mapping.getRole().name(), null, null, null);
    }

    /**
     * Resolves the union of Arbor roles mapped from the given IdP group names/object-ids. Unknown
     * groups (no mapping row) are silently ignored: safest default. Match is case-insensitive.
     */
    @Transactional(readOnly = true)
    public Set<Role> resolveRoles(List<String> idpGroupNames) {
        if (idpGroupNames == null || idpGroupNames.isEmpty()) {
            return EnumSet.noneOf(Role.class);
        }
        Set<String> wanted = idpGroupNames.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase())
                .collect(Collectors.toSet());
        if (wanted.isEmpty()) {
            return EnumSet.noneOf(Role.class);
        }
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (SsoGroupRoleMapping m : repository.findAllByOrderByIdpGroupNameAsc()) {
            if (wanted.contains(m.getIdpGroupName().toLowerCase())) {
                roles.add(m.getRole());
            }
        }
        return roles;
    }

    /** Tenant SSO mappings may not grant platform or vendor-only roles. */
    static boolean isMappableRole(Role role) {
        return role != Role.SUPER_ADMIN && role != Role.VENDOR;
    }

    private static String primaryRole(User user) {
        return user.getRoles().stream().map(Role::name).sorted().findFirst().orElse("UNKNOWN");
    }
}
