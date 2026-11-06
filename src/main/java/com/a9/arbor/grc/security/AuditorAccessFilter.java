package com.a9.arbor.grc.security;

import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;
import com.a9.arbor.grc.modules.auditmgmt.service.AuditAccessGrantService;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Auditor login ({@code INTERNAL_AUDITOR} / leftover {@code EXTERNAL_AUDITOR}) uses the same app
 * shell as Admin. Reads are limited to modules on an active Access Grant. Writes are limited to
 * assigned-audit fieldwork (checklist, findings, scope, lead sign). CAPA and other GRC modules stay view-only.
 */
@Component
public class AuditorAccessFilter extends OncePerRequestFilter {

    private static final Pattern FIELDWORK_WRITE = Pattern.compile(
            "^/api/audits/\\d+/(start|sync-gap-scores|domain-narratives|checklist(/.*)?|checklists(/.*)?|findings(/.*)?|summary|scope|auditor-sign)$");

    private final UserService userService;
    private final AuditAccessGrantService accessGrantService;

    public AuditorAccessFilter(UserService userService, AuditAccessGrantService accessGrantService) {
        this.userService = userService;
        this.accessGrantService = accessGrantService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = servletPath(request);
        if (!path.startsWith("/api/") || starts(path, "/api/auth") || starts(path, "/api/health")
                || starts(path, "/api/webhooks/malware-scan")) {
            filterChain.doFilter(request, response);
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !isAuditorConsole(auth)) {
            filterChain.doFilter(request, response);
            return;
        }
        // Grants live in the tenant DB. A token with no tenant must not call findByEmail
        // (500 left the SPA stuck signed in and blocked the login screen).
        if (!TenantContext.hasTenant()) {
            unauthorised(response, "Auditor session is not valid");
            return;
        }

        User user = userService.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            unauthorised(response, "Auditor session is not valid");
            return;
        }
        Set<EngagementScopeModule> granted = accessGrantService.grantedModulesForUser(user);
        String method = request.getMethod();
        boolean get = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);

        if (get) {
            if (isDataExport(path)) {
                forbid(response, "Auditor access is view-only (export is not allowed)");
                return;
            }
            if (getAllowed(path, granted)) {
                filterChain.doFilter(request, response);
                return;
            }
            forbid(response, "This module is not on an active access grant");
            return;
        }

        if (isFieldworkWrite(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (starts(path, "/api/notifications")
                && !path.contains("template")
                && !path.contains("email-outbox")) {
            filterChain.doFilter(request, response);
            return;
        }
        forbid(response, "Auditor access is view-only except assigned audit fieldwork");
    }

    private static boolean isAuditorConsole(Authentication auth) {
        boolean auditor = false;
        boolean elevated = false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            String r = a.getAuthority();
            if ("ROLE_INTERNAL_AUDITOR".equals(r) || "ROLE_EXTERNAL_AUDITOR".equals(r)) {
                auditor = true;
            }
            if ("ROLE_OWNER".equals(r) || "ROLE_ADMIN".equals(r) || "ROLE_SUPER_ADMIN".equals(r)) {
                elevated = true;
            }
        }
        return auditor && !elevated;
    }

    private static boolean getAllowed(String path, Set<EngagementScopeModule> granted) {
        if (starts(path, "/api/auth")
                || starts(path, "/api/modules")
                || starts(path, "/api/audits")
                || starts(path, "/api/notifications")
                || starts(path, "/api/org")
                || starts(path, "/api/frameworks")
                || starts(path, "/api/capa")
                || path.equals("/api/users/directory")) {
            return true;
        }
        if (starts(path, "/api/dashboard")) {
            return granted.contains(EngagementScopeModule.DASHBOARD);
        }
        if (starts(path, "/api/gap-assessments")) {
            return granted.contains(EngagementScopeModule.GAP);
        }
        if (starts(path, "/api/soa")) {
            return granted.contains(EngagementScopeModule.SOA);
        }
        if (starts(path, "/api/compliance-projects")) {
            return granted.contains(EngagementScopeModule.GAP) || granted.contains(EngagementScopeModule.SOA);
        }
        if (path.equals("/api/risks/heatmap") || path.equals("/api/risks/matrices")) {
            return granted.contains(EngagementScopeModule.DASHBOARD)
                    || granted.contains(EngagementScopeModule.RISK)
                    || granted.contains(EngagementScopeModule.ERM);
        }
        if (starts(path, "/api/assets")) {
            return granted.contains(EngagementScopeModule.ASSET);
        }
        if (starts(path, "/api/risks") || starts(path, "/api/risk-projects")) {
            return granted.contains(EngagementScopeModule.RISK) || granted.contains(EngagementScopeModule.ERM);
        }
        if (starts(path, "/api/evidence")) {
            return granted.contains(EngagementScopeModule.EVIDENCE);
        }
        if (starts(path, "/api/bcms")) {
            return granted.contains(EngagementScopeModule.BCMS);
        }
        if (starts(path, "/api/privacy")) {
            return granted.contains(EngagementScopeModule.PRIVACY);
        }
        if (starts(path, "/api/tprm") || starts(path, "/api/kris") || starts(path, "/api/incidents")) {
            return granted.contains(EngagementScopeModule.ERM);
        }
        return false;
    }

    static boolean isBlankChecklistTemplate(String path) {
        return "/api/audits/checklist-library/template.xlsx".equals(path);
    }

    static boolean isDataExport(String path) {
        if (isBlankChecklistTemplate(path)) {
            return false;
        }
        return path.contains("/export") || path.endsWith("/template.xlsx");
    }

    private static boolean starts(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    static boolean isFieldworkWrite(String path) {
        return path != null && FIELDWORK_WRITE.matcher(path).matches();
    }

    static String servletPath(HttpServletRequest request) {
        String servlet = request.getServletPath();
        String info = request.getPathInfo();
        String p;
        if (servlet != null && !servlet.isBlank() && info != null && !info.isBlank()) {
            p = servlet.endsWith("/")
                    ? servlet.substring(0, servlet.length() - 1) + info
                    : servlet + info;
        } else if (servlet != null && !servlet.isBlank()) {
            p = servlet;
        } else {
            p = request.getRequestURI();
            String ctx = request.getContextPath();
            if (p != null && ctx != null && !ctx.isBlank() && p.startsWith(ctx)) {
                p = p.substring(ctx.length());
            }
        }
        if (p == null) {
            return "";
        }
        int q = p.indexOf('?');
        if (q >= 0) {
            p = p.substring(0, q);
        }
        if (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private static void unauthorised(HttpServletResponse response, String message) throws IOException {
        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, message);
    }

    private static void forbid(HttpServletResponse response, String message) throws IOException {
        writeJson(response, HttpServletResponse.SC_FORBIDDEN, message);
    }

    private static void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"message\":\"" + escaped + "\"}");
    }
}
