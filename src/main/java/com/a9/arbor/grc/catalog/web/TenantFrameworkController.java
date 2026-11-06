package com.a9.arbor.grc.catalog.web;

import com.a9.arbor.grc.catalog.service.CrossFrameworkMappingService;
import com.a9.arbor.grc.catalog.service.FrameworkLibraryService;
import com.a9.arbor.grc.catalog.service.FrameworkQuestionService;
import com.a9.arbor.grc.catalog.web.dto.ActivateFrameworkRequest;
import com.a9.arbor.grc.catalog.web.dto.CrossFrameworkMappingResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkDetailResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkQuestionResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkSummaryResponse;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/frameworks")
public class TenantFrameworkController {

    private final FrameworkLibraryService frameworkLibraryService;
    private final CrossFrameworkMappingService crossFrameworkMappingService;
    private final FrameworkQuestionService frameworkQuestionService;

    public TenantFrameworkController(FrameworkLibraryService frameworkLibraryService,
                                     CrossFrameworkMappingService crossFrameworkMappingService,
                                     FrameworkQuestionService frameworkQuestionService) {
        this.frameworkLibraryService = frameworkLibraryService;
        this.crossFrameworkMappingService = crossFrameworkMappingService;
        this.frameworkQuestionService = frameworkQuestionService;
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<FrameworkSummaryResponse> available() {
        return frameworkLibraryService.listAvailableForTenant(requireTenant());
    }

    @GetMapping("/activated")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<FrameworkSummaryResponse> activated() {
        return frameworkLibraryService.listActivatedForTenant(requireTenant());
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public FrameworkSummaryResponse activate(@PathVariable Long id,
                                             @RequestBody(required = false) ActivateFrameworkRequest body) {
        boolean includeCloud = body != null && Boolean.TRUE.equals(body.includeCloud());
        return frameworkLibraryService.activate(requireTenant(), id, currentEmail(), includeCloud);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void deactivate(@PathVariable Long id) {
        frameworkLibraryService.deactivate(requireTenant(), id);
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public FrameworkDetailResponse detail(@PathVariable Long id) {
        return frameworkLibraryService.getForTenant(requireTenant(), id);
    }

    @GetMapping("/{id:\\d+}/nodes/{nodeId}/questions")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<FrameworkQuestionResponse> nodeQuestions(@PathVariable Long id, @PathVariable Long nodeId) {
        frameworkLibraryService.getForTenant(requireTenant(), id);
        return frameworkQuestionService.listActiveForNode(id, nodeId);
    }

    @GetMapping("/nodes/{nodeId}/cross-mappings")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<CrossFrameworkMappingResponse> crossMappings(@PathVariable Long nodeId) {
        return crossFrameworkMappingService.listForNode(nodeId);
    }

    private static String requireTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant context required");
        }
        return tenantId;
    }

    private static String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }
}
