package com.a9.arbor.grc.modules.iam.web.dto;

import com.a9.arbor.grc.modules.iam.domain.User;

/** Slim tenant directory for pickers (id, name, email). No roles or status. */
public record UserDirectoryResponse(Long id, String email, String fullName) {
    public static UserDirectoryResponse from(User user) {
        return new UserDirectoryResponse(user.getId(), user.getEmail(), user.getFullName());
    }
}
