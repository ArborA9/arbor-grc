package com.a9.arbor.grc.modules.auditmgmt.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.auditmgmt.service.AuditPortalService;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditPortalEvidenceItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditPortalGapItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditPortalRiskItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditPortalSessionResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditPortalSoaItemResponse;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.security.RequiresModule;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/audits/portal")
@RequiresModule(ModuleCode.AUDIT)
public class AuditPortalController {

    private final AuditPortalService portalService;
    private final UserService userService;

    public AuditPortalController(AuditPortalService portalService, UserService userService) {
        this.portalService = portalService;
        this.userService = userService;
    }

    @GetMapping("/session")
    @PreAuthorize("hasRole('EXTERNAL_AUDITOR')")
    public AuditPortalSessionResponse session(Authentication authentication) {
        return portalService.session(current(authentication));
    }

    @GetMapping("/{auditId}/gap")
    @PreAuthorize("hasRole('EXTERNAL_AUDITOR')")
    public List<AuditPortalGapItemResponse> gapItems(Authentication authentication, @PathVariable long auditId) {
        return portalService.listGapItems(current(authentication), auditId);
    }

    @GetMapping("/{auditId}/soa")
    @PreAuthorize("hasRole('EXTERNAL_AUDITOR')")
    public List<AuditPortalSoaItemResponse> soaItems(Authentication authentication, @PathVariable long auditId) {
        return portalService.listSoaItems(current(authentication), auditId);
    }

    @GetMapping("/{auditId}/risks")
    @PreAuthorize("hasRole('EXTERNAL_AUDITOR')")
    public List<AuditPortalRiskItemResponse> risks(Authentication authentication, @PathVariable long auditId) {
        return portalService.listRisks(current(authentication), auditId);
    }

    @GetMapping("/{auditId}/evidence")
    @PreAuthorize("hasRole('EXTERNAL_AUDITOR')")
    public List<AuditPortalEvidenceItemResponse> evidence(Authentication authentication, @PathVariable long auditId) {
        return portalService.listEvidence(current(authentication), auditId);
    }

    private User current(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
