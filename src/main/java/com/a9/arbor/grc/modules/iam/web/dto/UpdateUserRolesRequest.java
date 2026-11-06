package com.a9.arbor.grc.modules.iam.web.dto;

import com.a9.arbor.grc.modules.iam.domain.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

/** {@code orgUnitIds} omitted (null) keeps the user's current departments. */
public record UpdateUserRolesRequest(@NotEmpty Set<Role> roles, List<String> adminModules,
                                     List<@NotNull Long> orgUnitIds) {
}
