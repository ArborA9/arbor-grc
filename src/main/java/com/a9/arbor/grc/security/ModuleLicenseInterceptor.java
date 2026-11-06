package com.a9.arbor.grc.security;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.service.LicenseExpiryCopy;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
import com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService;
import com.a9.arbor.grc.tenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class ModuleLicenseInterceptor implements HandlerInterceptor {

    private final ModuleLicensingService moduleLicensingService;
    private final AdminModuleAccessService adminModuleAccessService;

    public ModuleLicenseInterceptor(ModuleLicensingService moduleLicensingService,
                                    AdminModuleAccessService adminModuleAccessService) {
        this.moduleLicensingService = moduleLicensingService;
        this.adminModuleAccessService = adminModuleAccessService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        adminModuleAccessService.prepareCurrentUser();
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequiresModule ann = method.getMethodAnnotation(RequiresModule.class);
        if (ann == null) {
            ann = method.getBeanType().getAnnotation(RequiresModule.class);
        }
        if (ann == null) {
            return true;
        }

        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant context required for module check");
        }

        ModuleCode[] codes = ann.value();
        if (codes.length == 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "RequiresModule has no modules");
        }
        boolean anyEnabled = false;
        boolean anyWritable = false;
        for (ModuleCode code : codes) {
            if (moduleLicensingService.isEnabled(tenantId, code)) {
                anyEnabled = true;
                if (moduleLicensingService.isWritable(tenantId, code)) {
                    anyWritable = true;
                }
            }
        }
        if (!anyEnabled) {
            String names = Arrays.stream(codes).map(Enum::name).collect(Collectors.joining(" or "));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, LicenseExpiryCopy.notLicensed(names));
        }
        String httpMethod = request.getMethod();
        boolean read = "GET".equalsIgnoreCase(httpMethod) || "HEAD".equalsIgnoreCase(httpMethod);
        boolean download = read && LicenseWriteLockFilter.isDownloadOrExport(AuditorAccessFilter.servletPath(request));
        if ((read && !download) || anyWritable) {
            adminModuleAccessService.assertMayUseAny(codes, download || !read);
            return true;
        }
        String expired = Arrays.stream(codes)
                .filter(c -> moduleLicensingService.isModuleExpired(tenantId, c))
                .map(Enum::name)
                .collect(Collectors.joining(" and "));
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                LicenseExpiryCopy.writeForbidden(expired.isBlank() ? codes[0].name() : expired));
    }
}
