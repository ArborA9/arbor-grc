package com.a9.arbor.grc.config;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

/**
 * Browser document routes for the React console (not {@code /api}, {@code /sso}, Vite {@code /assets/*}).
 * Asset register is {@code /asset-register} so it does not collide with hashed JS under {@code /assets/}.
 */
public final class SpaRoutes {

    public static final Set<String> APP_SEGMENTS = Set.of(
            "dashboard",
            "contributor-dashboard",
            "asset-register",
            "frameworks",
            "compliance",
            "assessments",
            "assessments-projects",
            "soa",
            "risk-manager",
            "risk",
            "erm-risk",
            "tprm",
            "tprm-projects",
            "tprm-vendors",
            "tprm-questionnaires",
            "kri",
            "incidents",
            "audit",
            "audit-register",
            "audit-engagements",
            "audit-checklists",
            "audit-capa",
            "audit-findings",
            "audit-register2",
            "audit-register3",
            "audit-grants",
            "audit-access-log",
            "business-continuity",
            "bcms-profiles",
            "bcms-bia",
            "bcms-plans",
            "bcms-exercises",
            "bcms-ncema",
            "privacy",
            "privacy-inventory",
            "privacy-ropa",
            "privacy-dsr",
            "privacy-dpia",
            "privacy-consent",
            "privacy-breach",
            "evidence",
            "policies",
            "reports",
            "settings",
            "users",
            "organisation",
            "notifications",
            "audit-trail",
            "workflows"
    );

    private SpaRoutes() {
    }

    public static boolean isPublicDocumentGet(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return false;
        }
        return isDocumentPath(request.getRequestURI());
    }

    public static boolean isDocumentPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return false;
        }
        int q = rawPath.indexOf('?');
        String path = q >= 0 ? rawPath.substring(0, q) : rawPath;
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.equals("/signin") || path.startsWith("/signin/") || path.equals("/login")
                || path.equals("/ops/signin")
                || path.equals("/vendor-portal") || path.startsWith("/vendor-portal/")
                || path.equals("/external-audit") || path.startsWith("/external-audit/")) {
            return true;
        }
        if (path.startsWith("/modules/")) {
            return path.indexOf('/', "/modules/".length()) < 0;
        }
        if (path.startsWith("/") && path.indexOf('/', 1) < 0) {
            return APP_SEGMENTS.contains(path.substring(1));
        }
        return false;
    }
}
