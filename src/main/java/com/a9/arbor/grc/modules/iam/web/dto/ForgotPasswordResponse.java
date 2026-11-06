package com.a9.arbor.grc.modules.iam.web.dto;

/**
 * @param previewLink reset link echoed only when email is not sending on this host (local / E2E)
 */
public record ForgotPasswordResponse(String kind, String previewLink) {
    public static ForgotPasswordResponse local(String previewLink) {
        return new ForgotPasswordResponse("LOCAL", previewLink);
    }

    public static ForgotPasswordResponse sso() {
        return new ForgotPasswordResponse("SSO", null);
    }

    public static ForgotPasswordResponse notLocal() {
        return new ForgotPasswordResponse("NOT_LOCAL", null);
    }
}
