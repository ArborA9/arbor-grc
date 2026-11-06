package com.a9.arbor.grc.modules.iam.web.dto;

import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;

import java.time.Instant;
import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String authType,
        boolean localLoginEnabled,
        String status,
        List<String> roles,
        List<Long> orgUnitIds,
        List<String> adminModules,
        Instant createdAt
) {
    public static UserResponse from(User user, List<Long> orgUnitIds, List<String> adminModules) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAuthType().name(),
                user.isLocalLoginEnabled(),
                user.getStatus().name(),
                user.getRoles().stream().map(Role::name).sorted().toList(),
                orgUnitIds,
                adminModules == null ? List.of() : adminModules,
                user.getCreatedAt());
    }

    public static UserResponse from(User user) {
        return from(user, List.of(), List.of());
    }
}
