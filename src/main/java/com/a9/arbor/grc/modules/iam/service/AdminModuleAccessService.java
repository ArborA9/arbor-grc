package com.a9.arbor.grc.modules.iam.service;

import com.a9.arbor.grc.catalog.domain.Framework;
import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.repo.FrameworkRepository;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserAdminModule;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.UserAdminModuleRepository;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Module list for an Admin who is not an Owner. Owner keeps every licensed module.
 * Users, organisation, and notifications are not modules and stay available.
 */
@Service
public class AdminModuleAccessService {

    private final UserRepository userRepository;
    private final UserAdminModuleRepository moduleRepository;
    private final ModuleLicensingService moduleLicensingService;
    private final FrameworkRepository frameworkRepository;
    private final AdminModuleAccessService self;

    public AdminModuleAccessService(UserRepository userRepository,
                                    UserAdminModuleRepository moduleRepository,
                                    ModuleLicensingService moduleLicensingService,
                                    FrameworkRepository frameworkRepository,
                                    @Lazy AdminModuleAccessService self) {
        this.userRepository = userRepository;
        this.moduleRepository = moduleRepository;
        this.moduleLicensingService = moduleLicensingService;
        this.frameworkRepository = frameworkRepository;
        this.self = self;
    }

    /** Admin without Owner. Owner wins and the module list is ignored. */
    public boolean isScoped(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().contains(Role.ADMIN) && !user.getRoles().contains(Role.OWNER);
    }

    /**
     * Read access is not limited when the person also holds a role that already reads every
     * licensed module. Writes still follow the Admin module list.
     */
    public boolean hasBroadRead(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().contains(Role.GLOBAL_READER)
                || user.getRoles().contains(Role.INTERNAL_AUDITOR)
                || user.getRoles().contains(Role.EXTERNAL_AUDITOR);
    }

    /** Navigation and read lists hide modules this Admin was not given. */
    public boolean restrictsReads() {
        User user = currentUser();
        return user != null && isScoped(user) && !hasBroadRead(user);
    }

    public boolean mayUse(ModuleCode code, boolean write) {
        if (code == null) {
            return false;
        }
        User user = currentUser();
        if (user == null || !isScoped(user)) {
            return true;
        }
        if (!write && hasBroadRead(user)) {
            return true;
        }
        return grants(user).contains(code);
    }

    /** Workflow and report codes. PLATFORM and unknown codes stay visible. */
    public boolean mayUseRaw(String raw, boolean write) {
        if (raw == null || raw.isBlank() || "PLATFORM".equalsIgnoreCase(raw.trim())) {
            return true;
        }
        try {
            return mayUse(ModuleCode.valueOf(raw.trim().toUpperCase(Locale.ROOT)), write);
        } catch (IllegalArgumentException ex) {
            return true;
        }
    }

    public boolean mayUseFramework(Long frameworkId, boolean write) {
        User user = currentUser();
        if (user == null || !isScoped(user)) {
            return true;
        }
        if (!write && hasBroadRead(user)) {
            return true;
        }
        if (frameworkId == null) {
            return false;
        }
        Framework framework = frameworkRepository.findById(frameworkId).orElse(null);
        if (framework == null || framework.getModuleCode() == null) {
            return false;
        }
        return grants(user).contains(framework.getModuleCode());
    }

    public void assertMayUse(ModuleCode code, boolean write) {
        if (!mayUse(code, write)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have access to module " + code.name());
        }
    }

    public void assertMayUseAny(ModuleCode[] codes, boolean write) {
        if (codes == null || codes.length == 0) {
            return;
        }
        for (ModuleCode code : codes) {
            if (mayUse(code, write)) {
                return;
            }
        }
        String names = java.util.Arrays.stream(codes).map(Enum::name).reduce((a, b) -> a + " or " + b).orElse("");
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to module " + names);
    }

    public Set<ModuleCode> retainLicensed(Set<ModuleCode> licensed) {
        if (licensed == null || licensed.isEmpty() || !restrictsReads()) {
            return licensed == null ? Set.of() : licensed;
        }
        User user = currentUser();
        if (user == null) {
            return licensed;
        }
        Set<ModuleCode> allowed = grants(user);
        EnumSet<ModuleCode> out = EnumSet.noneOf(ModuleCode.class);
        for (ModuleCode code : licensed) {
            if (allowed.contains(code)) {
                out.add(code);
            }
        }
        return out;
    }

    /**
     * Module codes this caller may assign. Null means every licensed module (Owner, or not an Admin).
     */
    public Set<String> visibleModuleCodes() {
        if (!restrictsReads()) {
            return null;
        }
        User user = currentUser();
        if (user == null) {
            return null;
        }
        Set<String> out = new TreeSet<>();
        for (ModuleCode code : grants(user)) {
            out.add(code.name());
        }
        return out;
    }

    /** Stored modules for an Admin. Empty for Owner and for anyone who is not an Admin. */
    public List<String> codesFor(User user) {
        if (!isScoped(user)) {
            return List.of();
        }
        return grants(user).stream().map(Enum::name).sorted().toList();
    }

    /** Active Admins who hold this module. An Admin who is also Owner holds every module. */
    public List<Long> activeAdminIdsFor(ModuleCode code) {
        List<Long> out = new java.util.ArrayList<>();
        for (User user : userRepository.findByRolesContainingAndStatus(Role.ADMIN, UserStatus.ACTIVE)) {
            if (!isScoped(user) || grants(user).contains(code)) {
                out.add(user.getId());
            }
        }
        return out;
    }

    /**
     * Copies currently licensed modules onto an Admin whose list was never saved.
     * Runs in its own transaction so a read-only request can still persist the copy.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void prepare(long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.isAdminModulesSet() || !isScoped(user)) {
            return;
        }
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return;
        }
        moduleRepository.deleteByUserId(userId);
        for (String code : moduleLicensingService.enabledCodes(tenantId)) {
            UserAdminModule row = new UserAdminModule();
            row.setUserId(userId);
            row.setModuleCode(code);
            moduleRepository.save(row);
        }
        user.setAdminModulesSet(true);
        userRepository.save(user);
    }

    public void prepareCurrentUser() {
        if (!hasTenant()) {
            return;
        }
        User user = currentUser();
        if (user != null && isScoped(user) && !user.isAdminModulesSet()) {
            self.prepare(user.getId());
        }
    }

    /**
     * SSO-mapped Admin starts with no modules. An Owner assigns them before that person can work.
     */
    @Transactional
    public void holdWithNoModules(User user) {
        moduleRepository.deleteByUserId(user.getId());
        user.setAdminModulesSet(true);
    }

    @Transactional
    public void assign(User actor, User target, Set<Role> roles, List<String> requested) {
        boolean scoped = roles != null && roles.contains(Role.ADMIN) && !roles.contains(Role.OWNER);
        moduleRepository.deleteByUserId(target.getId());
        if (!scoped) {
            target.setAdminModulesSet(false);
            return;
        }
        Set<ModuleCode> codes = parseRequired(requested);
        String tenantId = requireTenant();
        Set<ModuleCode> grantable = grantable(actor, tenantId);
        for (ModuleCode code : codes) {
            if (!moduleLicensingService.isEnabled(tenantId, code)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Module " + code.name() + " is not licensed for this workspace");
            }
            if (!grantable.contains(code)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You can only assign modules you have");
            }
        }
        for (ModuleCode code : codes) {
            UserAdminModule row = new UserAdminModule();
            row.setUserId(target.getId());
            row.setModuleCode(code.name());
            moduleRepository.save(row);
        }
        target.setAdminModulesSet(true);
    }

    private Set<ModuleCode> grantable(User actor, String tenantId) {
        EnumSet<ModuleCode> enabled = EnumSet.noneOf(ModuleCode.class);
        for (String code : moduleLicensingService.enabledCodes(tenantId)) {
            enabled.add(ModuleCode.valueOf(code));
        }
        if (actor == null || !isScoped(actor)) {
            return enabled;
        }
        Set<ModuleCode> mine = grants(actor);
        mine.retainAll(enabled);
        return mine;
    }

    private Set<ModuleCode> grants(User user) {
        if (isScoped(user) && !user.isAdminModulesSet()) {
            self.prepare(user.getId());
        }
        EnumSet<ModuleCode> out = EnumSet.noneOf(ModuleCode.class);
        for (UserAdminModule row : moduleRepository.findByUserId(user.getId())) {
            try {
                ModuleCode code = ModuleCode.valueOf(row.getModuleCode());
                if (code.delivered()) {
                    out.add(code);
                }
            } catch (IllegalArgumentException ignored) {
                // Drop a code that is no longer a module.
            }
        }
        return out;
    }

    private Set<ModuleCode> parseRequired(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one module");
        }
        Set<ModuleCode> codes = new LinkedHashSet<>();
        for (String raw : requested) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            ModuleCode code;
            try {
                code = ModuleCode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown module: " + raw.trim());
            }
            if (!code.delivered()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown module: " + raw.trim());
            }
            codes.add(code);
        }
        if (codes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one module");
        }
        return codes;
    }

    private String requireTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant context required");
        }
        return tenantId;
    }

    private boolean hasTenant() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null && !tenantId.isBlank();
    }

    private User currentUser() {
        if (!hasTenant()) {
            return null;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()) {
            return null;
        }
        if ("anonymousUser".equals(auth.getName())) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
    }

    public Set<String> intersectEnabled(Collection<String> enabled) {
        Set<String> visible = visibleModuleCodes();
        if (visible == null || enabled == null) {
            return enabled == null ? Set.of() : new LinkedHashSet<>(enabled);
        }
        Set<String> out = new LinkedHashSet<>();
        for (String code : enabled) {
            if (visible.contains(code)) {
                out.add(code);
            }
        }
        return out;
    }
}
