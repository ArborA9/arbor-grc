package com.a9.arbor.grc.security;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.service.LicenseExpiryCopy;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
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

/**
 * Per-module write lock: an expired licensed module is view-only for that module's APIs.
 * Other licensed modules and platform APIs (users, org, dashboard) stay writable.
 * Mixed controllers (risk, Gap, SoA) are enforced in {@link ModuleLicenseInterceptor} / services.
 */
@Component
public class LicenseWriteLockFilter extends OncePerRequestFilter {

    private final ModuleLicensingService moduleLicensingService;

    public LicenseWriteLockFilter(ModuleLicensingService moduleLicensingService) {
        this.moduleLicensingService = moduleLicensingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = AuditorAccessFilter.servletPath(request);
        if (!path.startsWith("/api/") || isAlwaysOpen(path) || isSuperAdmin(SecurityContextHolder.getContext().getAuthentication())) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!TenantContext.hasTenant()) {
            filterChain.doFilter(request, response);
            return;
        }
        ModuleCode exclusive = LicenseApiModules.exclusiveModule(path);
        if (exclusive == null) {
            filterChain.doFilter(request, response);
            return;
        }
        String tenantId = TenantContext.getTenantId();
        if (!moduleLicensingService.isEnabled(tenantId, exclusive)
                || !moduleLicensingService.isModuleExpired(tenantId, exclusive)) {
            filterChain.doFilter(request, response);
            return;
        }
        String method = request.getMethod();
        boolean read = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
        if (read) {
            if (isDownloadOrExport(path)) {
                forbid(response, exclusive.name());
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }
        if (isAllowedWhileLocked(path, method)) {
            filterChain.doFilter(request, response);
            return;
        }
        forbid(response, exclusive.name());
    }

    static boolean isAlwaysOpen(String path) {
        return starts(path, "/api/auth")
                || starts(path, "/api/health")
                || starts(path, "/api/webhooks/malware-scan")
                || starts(path, "/api/admin")
                || starts(path, "/api/audits/external-access")
                || starts(path, "/api/tprm/vendor-access");
    }

    static boolean isAllowedWhileLocked(String path, String method) {
        if (starts(path, "/api/license/banner-dismiss")) {
            return true;
        }
        if (starts(path, "/api/notifications")
                && !path.contains("template")
                && !path.contains("email-outbox")) {
            return "POST".equalsIgnoreCase(method);
        }
        return false;
    }

    static boolean isDownloadOrExport(String path) {
        if (path == null) {
            return false;
        }
        if (AuditorAccessFilter.isBlankChecklistTemplate(path)) {
            return false;
        }
        String lower = path.toLowerCase();
        return lower.contains("/export")
                || lower.contains("/download")
                || lower.endsWith(".xlsx")
                || lower.endsWith(".csv")
                || lower.endsWith(".pdf")
                || lower.endsWith("/template.xlsx");
    }

    static boolean isSuperAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_SUPER_ADMIN".equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static boolean starts(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private static void forbid(HttpServletResponse response, String moduleCode) throws IOException {
        String message = LicenseExpiryCopy.writeForbidden(moduleCode).replace("\"", "'");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"" + message + "\",\"code\":\"" + LicenseExpiryCopy.CODE + "\"}");
    }
}
