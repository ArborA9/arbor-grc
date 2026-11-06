package com.a9.arbor.grc.modules.iam.web.dto;

/**
 * Arbor sign-out result. {@code endSessionUrl} is Entra's logout address for an SSO session
 * when the caller asked to end that session. {@code reauth} means the next SSO sign-in must
 * send {@code prompt=login}.
 */
public record LogoutResponse(String endSessionUrl, boolean reauth) {
}
