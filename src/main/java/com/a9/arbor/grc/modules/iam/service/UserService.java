package com.a9.arbor.grc.modules.iam.service;

import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.AuthType;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserOrgUnit;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.UserOrgUnitRepository;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.org.domain.OrgUnit;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.a9.arbor.grc.security.PasswordPolicyService;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Tenant-scoped user management. All operations run against the current tenant's database. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserOrgUnitRepository userOrgUnitRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditTrailService auditTrailService;
    private final NotificationService notificationService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordPolicyService passwordPolicyService;
    private final TenantRepository tenantRepository;
    private final ArborSecurityProperties securityProperties;
    private final AdminModuleAccessService adminModuleAccessService;

    public UserService(UserRepository userRepository,
                       UserOrgUnitRepository userOrgUnitRepository,
                       OrgUnitRepository orgUnitRepository,
                       PasswordEncoder passwordEncoder,
                       AuditTrailService auditTrailService,
                       NotificationService notificationService,
                       RefreshTokenService refreshTokenService,
                       PasswordPolicyService passwordPolicyService,
                       TenantRepository tenantRepository,
                       ArborSecurityProperties securityProperties,
                       AdminModuleAccessService adminModuleAccessService) {
        this.userRepository = userRepository;
        this.userOrgUnitRepository = userOrgUnitRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditTrailService = auditTrailService;
        this.notificationService = notificationService;
        this.refreshTokenService = refreshTokenService;
        this.passwordPolicyService = passwordPolicyService;
        this.tenantRepository = tenantRepository;
        this.securityProperties = securityProperties;
        this.adminModuleAccessService = adminModuleAccessService;
    }

    /** Creates a LOCAL (email/password) account: used for break-glass / bootstrap admins. */
    @Transactional
    public User createUser(String email, String rawPassword, String fullName, Set<Role> roles,
                           List<Long> orgUnitIds, List<String> adminModules) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            throw new UserAlreadyExistsException(normalized);
        }
        rejectVendorRoleAssignment(roles);
        passwordPolicyService.assertValid(rawPassword);
        User user = new User();
        user.setEmail(normalized);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setAuthType(AuthType.LOCAL);
        user.setLocalLoginEnabled(true);
        user.setStatus(UserStatus.ACTIVE);
        assertLocalRoleAllowed(roles);
        user.setRoles(roles);
        user = userRepository.save(user);
        adminModuleAccessService.assign(resolveActorUser(), user, roles, adminModules);
        replaceOrgUnits(user, orgUnitIds);
        Actor actor = resolveActor();
        auditTrailService.recordUserCreated(actor.userId(), actor.roleSnapshot(), user.getId(),
                user.getEmail(), AuthType.LOCAL.name());
        notifyUserInvited(user);
        return user;
    }

    /**
     * Pre-invites an SSO user: identity will come from the tenant's AD, roles are assigned here.
     * No password is stored. The account stays {@link UserStatus#INVITED} until the user first signs
     * in via SSO (which flips it to {@code ACTIVE}). An unknown SSO email is denied at login time.
     */
    @Transactional
    public User inviteSsoUser(String email, String fullName, Set<Role> roles, List<Long> orgUnitIds,
                               List<String> adminModules) {
        rejectVendorRoleAssignment(roles);
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            throw new UserAlreadyExistsException(normalized);
        }
        User user = new User();
        user.setEmail(normalized);
        user.setPasswordHash(null);
        user.setFullName(fullName);
        user.setAuthType(AuthType.SSO);
        user.setLocalLoginEnabled(false);
        user.setStatus(UserStatus.INVITED);
        user.setRoles(roles);
        user = userRepository.save(user);
        adminModuleAccessService.assign(resolveActorUser(), user, roles, adminModules);
        replaceOrgUnits(user, orgUnitIds);
        Actor actor = resolveActor();
        auditTrailService.recordUserCreated(actor.userId(), actor.roleSnapshot(), user.getId(),
                user.getEmail(), AuthType.SSO.name());
        notifyUserInvited(user);
        return user;
    }

    /**
     * JIT-provisions a new SSO user on first login when the tenant's IdP config allows it
     * (Path C B1). Created {@code ACTIVE} immediately (identity already proven by the IdP) with no
     * organisation units: an Admin assigns those afterwards. Always notifies Admins so the
     * auto-created account gets reviewed.
     */
    @Transactional
    public User provisionJitUser(String email, Role defaultRole) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            throw new UserAlreadyExistsException(normalized);
        }
        User user = new User();
        user.setEmail(normalized);
        user.setPasswordHash(null);
        user.setFullName(normalized);
        user.setAuthType(AuthType.SSO);
        user.setLocalLoginEnabled(false);
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(new LinkedHashSet<>(Set.of(defaultRole)));
        user = userRepository.save(user);
        if (defaultRole == Role.ADMIN) {
            adminModuleAccessService.holdWithNoModules(user);
        }
        auditTrailService.recordUserCreated(null, "SYSTEM(JIT)", user.getId(), user.getEmail(), AuthType.SSO.name());
        notificationService.trigger(
                "SSO_JIT_PROVISIONED",
                user.getId().toString(),
                List.of("ADMIN"),
                Map.of("email", user.getEmail(), "role", defaultRole.name()));
        return user;
    }

    /**
     * Applies IdP group -> role mappings as a <b>union</b> with the user's existing Arbor-assigned
     * roles (Path C B2): never removes a role an Admin assigned directly. Persists only when the
     * effective role set actually changes.
     */
    @Transactional
    public User unionRoles(User user, Set<Role> additionalRoles) {
        if (additionalRoles == null || additionalRoles.isEmpty()) {
            return user;
        }
        boolean hadAdmin = user.getRoles().contains(Role.ADMIN);
        Set<Role> merged = new LinkedHashSet<>(user.getRoles());
        if (merged.addAll(additionalRoles)) {
            user.setRoles(merged);
            if (!hadAdmin && merged.contains(Role.ADMIN) && !merged.contains(Role.OWNER)) {
                adminModuleAccessService.holdWithNoModules(user);
            }
            userRepository.save(user);
        }
        return user;
    }

    @Transactional
    public User replaceOrgUnits(User user, List<Long> orgUnitIds) {
        List<Long> ids = orgUnitIds == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(orgUnitIds));
        if (user.getRoles().contains(Role.CONTRIBUTOR) && ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contributors must be assigned to at least one organisation unit");
        }
        for (Long unitId : ids) {
            if (!orgUnitRepository.existsById(unitId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown organisation unit: " + unitId);
            }
        }
        userOrgUnitRepository.deleteByUserId(user.getId());
        for (Long unitId : ids) {
            UserOrgUnit link = new UserOrgUnit();
            link.setUserId(user.getId());
            link.setOrgUnitId(unitId);
            userOrgUnitRepository.save(link);
        }
        return user;
    }

    private String orgUnitNames(List<Long> ids) {
        if (ids.isEmpty()) {
            return "-";
        }
        return orgUnitRepository.findAllById(ids).stream()
                .map(OrgUnit::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
    }

    @Transactional(readOnly = true)
    public List<Long> orgUnitIdsFor(Long userId) {
        return userOrgUnitRepository.findByUserId(userId).stream()
                .map(UserOrgUnit::getOrgUnitId)
                .sorted()
                .toList();
    }

    private void notifyUserInvited(User user) {
        // Arbor 10 Oct 2026: no welcome mail or bell when the new account is an Admin or a Contributor.
        if (user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.CONTRIBUTOR)) {
            return;
        }
        String roles = user.getRoles().stream().map(Role::name).sorted().collect(Collectors.joining(", "));
        notificationService.trigger(
                "USER_INVITED",
                user.getId().toString(),
                List.of(user.getId().toString()),
                Map.of(
                        "roles", roles,
                        "authType", user.getAuthType().name(),
                        "email", user.getEmail()));
    }

    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    /** Enabled internal tenant users for pickers (Owner, Admin, Contributor, Global Reader). Excludes vendors and auditors. */
    @Transactional(readOnly = true)
    public List<User> listDirectoryUsers() {
        // Exclude external roles: vendors (TPRM portal) and auditors (IA console / external portal)
        var excludedRoles = java.util.Set.of(Role.INTERNAL_AUDITOR, Role.EXTERNAL_AUDITOR, Role.VENDOR);
        return userRepository.findAll().stream()
                .filter(u -> u.getStatus() != UserStatus.DISABLED)
                .filter(u -> u.getRoles().stream().noneMatch(excludedRoles::contains))
                .sorted(Comparator
                        .comparing(User::getFullName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(User::getEmail, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    /** Internal audit planning: lead and team pickers (Internal Auditor, Admin, Owner). */
    @Transactional(readOnly = true)
    public List<User> listAuditPlanningUsers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getStatus() != UserStatus.DISABLED)
                .filter(u -> u.getRoles().stream().anyMatch(r ->
                        r == Role.INTERNAL_AUDITOR || r == Role.ADMIN || r == Role.OWNER))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Completes an SSO login for the given email (identity just proven by the IdP). Only matches a
     * pre-invited {@code SSO} account: an unknown or disabled email returns empty (login denied).
     * A first-time login flips {@code INVITED → ACTIVE}. Runs in the current tenant's DB.
     */
    @Transactional
    public Optional<User> completeSsoLogin(String email) {
        String normalized = email == null ? null : email.trim().toLowerCase();
        if (normalized == null || normalized.isEmpty()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(normalized)
                .or(() -> userRepository.findByEmail(email.trim())) // tolerate legacy mixed-case invites
                .filter(u -> u.getStatus() != UserStatus.DISABLED)
                .map(u -> {
                    if (u.getStatus() == UserStatus.INVITED) {
                        u.setStatus(UserStatus.ACTIVE);
                        userRepository.save(u);
                    }
                    return u;
                });
    }

    public boolean matchesPassword(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    @Transactional
    public User disableUser(User actor, long targetId) {
        User target = requireUser(targetId);
        assertCanManage(actor, target);
        if (actor.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot deactivate your own account");
        }
        if (target.getStatus() == UserStatus.DISABLED) {
            return target;
        }
        assertNotLastOwner(target);
        target.setStatus(UserStatus.DISABLED);
        bumpTokenVersion(target);
        userRepository.save(target);
        refreshTokenService.revokeAllForUser(target.getId());
        notificationService.trigger(
                "USER_DEACTIVATED",
                target.getId().toString(),
                List.of(target.getId().toString()),
                Map.of("email", target.getEmail()));
        return target;
    }

    @Transactional
    public User enableUser(User actor, long targetId) {
        User target = requireUser(targetId);
        assertCanManage(actor, target);
        if (target.getStatus() != UserStatus.DISABLED) {
            return target;
        }
        target.setStatus(UserStatus.ACTIVE);
        return userRepository.save(target);
    }

    @Transactional
    public User updateRoles(User actor, long targetId, Set<Role> roles, List<String> adminModules) {
        return updateRoles(actor, targetId, roles, adminModules, null);
    }

    /** {@code orgUnitIds} null keeps the user's current departments. */
    @Transactional
    public User updateRoles(User actor, long targetId, Set<Role> roles, List<String> adminModules,
                            List<Long> orgUnitIds) {
        if (roles == null || roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one role is required");
        }
        User target = requireUser(targetId);
        assertCanManage(actor, target);
        assertVendorNotEditable(target);
        rejectVendorRoleAssignment(roles);
        List<Long> currentUnits = orgUnitIdsFor(target.getId());
        List<Long> nextUnits = orgUnitIds == null
                ? currentUnits
                : new LinkedHashSet<>(orgUnitIds).stream().sorted().toList();
        if (roles.contains(Role.CONTRIBUTOR) && nextUnits.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose at least one department for a Contributor");
        }
        Set<Role> previous = new LinkedHashSet<>(target.getRoles());
        if (!target.getRoles().contains(Role.OWNER) && roles.contains(Role.OWNER)
                && actor.getRoles().stream().noneMatch(r -> r == Role.OWNER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an Owner can grant Owner role");
        }
        if (previous.contains(Role.OWNER) && !roles.contains(Role.OWNER)) {
            assertNotLastOwner(target);
        }
        target.setRoles(new LinkedHashSet<>(roles));
        adminModuleAccessService.assign(actor, target, roles, adminModules);
        if (!nextUnits.equals(currentUnits)) {
            replaceOrgUnits(target, nextUnits);
            Actor auditActor = resolveActor();
            auditTrailService.record(AuditActionType.UPDATED, "IAM", auditActor.userId(), auditActor.roleSnapshot(),
                    "User", target.getId().toString(), target.getEmail(), "departments",
                    orgUnitNames(currentUnits), orgUnitNames(nextUnits), null, null);
        }
        if (previous.equals(roles)) {
            userRepository.save(target);
            return target;
        }
        bumpTokenVersion(target);
        userRepository.save(target);
        String roleList = roles.stream().map(Role::name).sorted().collect(Collectors.joining(", "));
        notificationService.trigger(
                "ROLE_CHANGED",
                target.getId().toString(),
                List.of(target.getId().toString()),
                Map.of("roles", roleList, "email", target.getEmail()));
        return target;
    }

    @Transactional
    public User resetPassword(User actor, long targetId, String rawPassword) {
        User target = requireUser(targetId);
        assertCanManage(actor, target);
        assertVendorNotEditable(target);
        return setLocalPassword(target, rawPassword);
    }

    /**
     * Sets a LOCAL user's password, invalidates outstanding JWTs, and revokes refresh tokens.
     */
    @Transactional
    public User setLocalPassword(User target, String rawPassword) {
        if (!target.isLocalLoginEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset applies to local login only");
        }
        passwordPolicyService.assertValid(rawPassword);
        target.setPasswordHash(passwordEncoder.encode(rawPassword));
        bumpTokenVersion(target);
        userRepository.save(target);
        refreshTokenService.revokeAllForUser(target.getId());
        return target;
    }

    private void bumpTokenVersion(User target) {
        target.setTokenVersion(target.getTokenVersion() + 1);
    }

    public User requireUser(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public void assertVendorNotEditable(User target) {
        if (target.getRoles() != null && target.getRoles().contains(Role.VENDOR)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vendor accounts cannot be edited here. Enable or disable the account instead");
        }
    }

    private void rejectVendorRoleAssignment(Set<Role> roles) {
        if (roles != null && roles.contains(Role.VENDOR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vendor accounts are created from Third-Party Risk, not Users");
        }
    }

    /**
     * Bootstrap Owner. {@code rawPassword} is null when the password will be set from an emailed link
     * or when the first sign-in is SSO.
     */
    @Transactional
    public User createProvisionOwner(String email, String fullName, String rawPassword, boolean ssoFirst) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            throw new UserAlreadyExistsException(normalized);
        }
        User user = new User();
        user.setEmail(normalized);
        user.setFullName(fullName.trim());
        user.setRoles(new LinkedHashSet<>(Set.of(Role.OWNER)));
        if (ssoFirst) {
            user.setAuthType(AuthType.SSO);
            user.setPasswordHash(null);
            user.setLocalLoginEnabled(false);
            user.setStatus(UserStatus.INVITED);
        } else {
            user.setAuthType(AuthType.LOCAL);
            user.setLocalLoginEnabled(true);
            user.setStatus(UserStatus.ACTIVE);
            if (rawPassword != null) {
                passwordPolicyService.assertValid(rawPassword);
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
            }
        }
        user = userRepository.save(user);
        auditTrailService.recordUserCreated(null, "SYSTEM", user.getId(), user.getEmail(), user.getAuthType().name());
        return user;
    }

    @Transactional
    public User disableLocalLogin(User actor, long targetId) {
        User target = requireUser(targetId);
        // Admin may turn local login off for an Owner. They still cannot deactivate the Owner
        // account, and they cannot turn that local login back on.
        if (!(actor.getRoles().contains(Role.ADMIN) && target.getRoles().contains(Role.OWNER))) {
            assertCanManage(actor, target);
        }
        if (!tenantSsoEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Turn on SSO before deactivating local login. It is the only way in.");
        }
        if (!target.isLocalLoginEnabled() && target.getPasswordHash() == null) {
            return target;
        }
        target.setLocalLoginEnabled(false);
        target.setPasswordHash(null);
        userRepository.save(target);
        refreshTokenService.revokeLocalForUser(target.getId());
        return target;
    }

    /**
     * Arms local login. Does not set a password. Caller emails a set-password link or calls
     * {@link #setLocalPassword} when mail is not live.
     */
    @Transactional
    public User enableLocalLogin(User actor, long targetId) {
        User target = requireUser(targetId);
        assertCanManage(actor, target);
        if (localLoginRestricted()) {
            if (!actor.getRoles().contains(Role.OWNER)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an Owner can turn local login back on");
            }
            if (!target.getRoles().contains(Role.OWNER)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Local login is limited to Owners while SSO is enabled");
            }
        }
        target.setLocalLoginEnabled(true);
        return userRepository.save(target);
    }

    public boolean localLoginRestricted() {
        return securityProperties.ownerOnlyLocalLogin() && tenantSsoEnabled();
    }

    public boolean tenantSsoEnabled() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }
        return tenantRepository.findByTenantId(tenantId).map(t -> t.isSsoEnabled()).orElse(false);
    }

    private void assertLocalRoleAllowed(Set<Role> roles) {
        if (!localLoginRestricted()) {
            return;
        }
        if (roles == null || !roles.contains(Role.OWNER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Local accounts are limited to Owners while SSO is enabled");
        }
    }

    private void assertCanManage(User actor, User target) {
        if (actor.getRoles().contains(Role.OWNER)) {
            return;
        }
        if (actor.getRoles().contains(Role.ADMIN)) {
            if (target.getRoles().contains(Role.OWNER)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins cannot manage Owner accounts");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
    }

    private void assertNotLastOwner(User target) {
        if (!target.getRoles().contains(Role.OWNER)) {
            return;
        }
        long owners = userRepository.findByRolesContainingAndStatus(Role.OWNER, UserStatus.ACTIVE).stream()
                .filter(u -> u.getStatus() != UserStatus.DISABLED)
                .count();
        if (owners <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot remove or deactivate the last Owner");
        }
    }

    private User resolveActorUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return findByEmail(auth.getName()).orElse(null);
    }

    private Actor resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return new Actor(null, "SYSTEM");
        }
        String email = auth.getName();
        Optional<User> actor = findByEmail(email);
        if (actor.isPresent()) {
            String roles = actor.get().getRoles().stream().map(Role::name).sorted()
                    .collect(Collectors.joining(","));
            return new Actor(actor.get().getId(), roles);
        }
        String roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .sorted()
                .collect(Collectors.joining(","));
        return new Actor(null, roles.isBlank() ? "UNKNOWN" : roles);
    }

    private record Actor(Long userId, String roleSnapshot) {
    }
}
