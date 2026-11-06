package com.a9.arbor.grc.modules.iam.web.dto;

/** Returned after a link sets the password, so the sign-in page can prefill the account. */
public record ResetPasswordResponse(String purpose, String email, String workspaceId) {
}
