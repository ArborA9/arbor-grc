package com.a9.arbor.grc.modules.iam.directory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the Entra directory (tenant) id from an OIDC issuer URL. Used to call Graph with the
 * same app registration already stored for SSO. Non-Entra issuers are rejected so we never
 * send the OIDC secret to {@code graph.microsoft.com} for an unrelated IdP.
 */
public final class EntraIssuer {

    private static final Pattern ENTRA_ISSUER = Pattern.compile(
            "(?i)https?://(?:login\\.microsoftonline\\.com|login\\.microsoft\\.com|sts\\.windows\\.net)/([^/\\s?#]+)");

    private EntraIssuer() {
    }

    /**
     * @return Entra tenant GUID or domain (path segment), empty when the issuer is not Entra
     *         or is a multi-tenant alias ({@code common} / {@code organizations} / {@code consumers}).
     */
    public static Optional<String> directoryId(String issuer) {
        if (issuer == null || issuer.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = ENTRA_ISSUER.matcher(issuer.trim());
        if (!matcher.find()) {
            return Optional.empty();
        }
        String tid = matcher.group(1);
        if ("common".equalsIgnoreCase(tid)
                || "organizations".equalsIgnoreCase(tid)
                || "consumers".equalsIgnoreCase(tid)) {
            return Optional.empty();
        }
        return Optional.of(tid);
    }

    /**
     * Entra v2 logout for this issuer. {@code postLogoutRedirect} must be registered on the app
     * as a Web redirect URI. Empty when the issuer is not a single Entra directory.
     */
    public static Optional<String> endSessionUrl(String issuer, String postLogoutRedirect) {
        if (postLogoutRedirect == null || postLogoutRedirect.isBlank()) {
            return Optional.empty();
        }
        Optional<String> directory = directoryId(issuer);
        if (directory.isEmpty()) {
            return Optional.empty();
        }
        String redirect = URLEncoder.encode(postLogoutRedirect.trim(), StandardCharsets.UTF_8);
        return Optional.of("https://login.microsoftonline.com/" + directory.get()
                + "/oauth2/v2.0/logout?post_logout_redirect_uri=" + redirect);
    }

    /** Strip quotes / slashes so the value is safe inside a Graph {@code $search} KQL fragment. */
    public static String sanitizeQuery(String query) {
        if (query == null) {
            return "";
        }
        String trimmed = query.trim();
        if (trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 100);
        }
        return trimmed.replaceAll("[\"'\\\\]", "").trim();
    }
}
