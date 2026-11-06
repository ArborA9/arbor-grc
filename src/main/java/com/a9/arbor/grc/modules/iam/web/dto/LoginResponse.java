package com.a9.arbor.grc.modules.iam.web.dto;

import java.util.List;

public record LoginResponse(
        String token,
        String email,
        String tenantId,
        List<String> roles,
        long expiresInMinutes,
        boolean mfaRequired,
        String mfaToken,
        String refreshToken,
        String authType
) {
    public static LoginResponse full(String token, String email, String tenantId,
                                     List<String> roles, long expiresInMinutes) {
        return full(token, email, tenantId, roles, expiresInMinutes, null, null);
    }

    public static LoginResponse full(String token, String email, String tenantId,
                                     List<String> roles, long expiresInMinutes, String refreshToken) {
        return full(token, email, tenantId, roles, expiresInMinutes, refreshToken, null);
    }

    public static LoginResponse full(String token, String email, String tenantId,
                                     List<String> roles, long expiresInMinutes, String refreshToken,
                                     String authType) {
        return new LoginResponse(token, email, tenantId, roles, expiresInMinutes, false, null, refreshToken, authType);
    }

    public static LoginResponse mfaChallenge(String email, String tenantId, String mfaToken) {
        return new LoginResponse(null, email, tenantId, List.of(), 0, true, mfaToken, null, null);
    }

    /** Browser cookie session: the SPA must not receive the raw tokens. */
    public LoginResponse withoutSecrets() {
        return new LoginResponse(null, email, tenantId, roles, expiresInMinutes, mfaRequired, mfaToken, null, authType);
    }
}
