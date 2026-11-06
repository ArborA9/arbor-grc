package com.a9.arbor.grc.modules.risk.service;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService;
import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Who gets the Admin copy of a risk email: the Admin who created the risk. When that person is
 * disabled or no longer an Admin of the risk's module, the module's Admins get it, and the
 * workspace Owner only when no Admin holds the module.
 */
@Component
public class RiskAdminRecipients {

    private final UserRepository userRepository;
    private final AdminModuleAccessService adminModuleAccessService;

    public RiskAdminRecipients(UserRepository userRepository, AdminModuleAccessService adminModuleAccessService) {
        this.userRepository = userRepository;
        this.adminModuleAccessService = adminModuleAccessService;
    }

    public List<String> forRisk(RiskRecord risk) {
        ModuleCode code = RiskModuleCodes.forType(risk.getRiskType());
        User creator = risk.getCreatedByUserId() == null ? null
                : userRepository.findById(risk.getCreatedByUserId()).orElse(null);
        if (creator != null && isAdminOf(creator, code)) {
            return List.of(String.valueOf(creator.getId()));
        }
        List<Long> ids = adminModuleAccessService.activeAdminIdsFor(code);
        if (ids.isEmpty()) {
            ids = userRepository.findByRolesContainingAndStatus(Role.OWNER, UserStatus.ACTIVE).stream()
                    .map(User::getId)
                    .toList();
        }
        return ids.stream().map(String::valueOf).toList();
    }

    private boolean isAdminOf(User user, ModuleCode code) {
        if (user.getStatus() != UserStatus.ACTIVE || user.getRoles() == null) {
            return false;
        }
        if (user.getRoles().contains(Role.OWNER)) {
            return true;
        }
        return user.getRoles().contains(Role.ADMIN)
                && (!adminModuleAccessService.isScoped(user)
                || adminModuleAccessService.codesFor(user).contains(code.name()));
    }
}
