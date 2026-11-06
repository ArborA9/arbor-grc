package com.a9.arbor.grc.modules.iam.web.dto;

import com.a9.arbor.grc.modules.iam.domain.AuthType;
import com.a9.arbor.grc.modules.iam.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Set;

/**
 * Create/invite a tenant user. Roles are always assigned here (Arbor manages authorization).
 *
 * <ul>
 *   <li>{@code authType = SSO} (default): a pre-invite: identity comes from the tenant's AD.
 *       {@code password} is ignored/not required.</li>
 *   <li>{@code authType = LOCAL}: a break-glass account: {@code password} (min 8 chars) is required.</li>
 * </ul>
 * Contributors should include at least one {@code orgUnitIds} (department) per req §5.2 / wizard.
 */
public record CreateUserRequest(
        @NotBlank @Email String email,
        String password,
        @NotBlank String fullName,
        AuthType authType,
        @NotEmpty Set<Role> roles,
        List<Long> orgUnitIds,
        List<String> adminModules
) {
    /** Defaults to SSO (AD-backed) when the caller omits the auth type. */
    public AuthType authTypeOrDefault() {
        return authType == null ? AuthType.SSO : authType;
    }
}
