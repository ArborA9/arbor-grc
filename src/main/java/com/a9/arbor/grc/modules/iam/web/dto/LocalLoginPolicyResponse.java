package com.a9.arbor.grc.modules.iam.web.dto;

/** How local passwords behave for this process and this tenant. */
public record LocalLoginPolicyResponse(String scope, boolean ssoEnabled, boolean ownerOnly) {
}
