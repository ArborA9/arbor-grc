package com.a9.arbor.grc.modules.iam.domain;

/**
 * How a user authenticates.
 *
 * <ul>
 *   <li>{@code SSO}: identity comes from the tenant's AD / Entra ID (OIDC). Arbor stores no
 *       password; the account is created by an admin (pre-invite) and linked by email on first login.</li>
 *   <li>{@code LOCAL}: email/password stored in Arbor. Used for break-glass / bootstrap accounts.</li>
 * </ul>
 */
public enum AuthType {
    SSO,
    LOCAL
}
