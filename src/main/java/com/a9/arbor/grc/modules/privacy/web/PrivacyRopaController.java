package com.a9.arbor.grc.modules.privacy.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.privacy.service.PrivacyRopaService;
import com.a9.arbor.grc.modules.privacy.web.dto.AssignRopaRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateRopaRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.RejectRopaRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.RopaResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.UpdateRopaRequest;
import com.a9.arbor.grc.security.RequiresModule;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/privacy/ropa")
@RequiresModule(ModuleCode.PRIVACY)
public class PrivacyRopaController {

    private static final String READ_ROLES =
            "hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')";

    private final PrivacyRopaService ropaService;
    private final UserService userService;

    public PrivacyRopaController(PrivacyRopaService ropaService, UserService userService) {
        this.ropaService = ropaService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize(READ_ROLES)
    public List<RopaResponse> list() {
        return ropaService.list();
    }

    @GetMapping("/template.xlsx")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public ResponseEntity<byte[]> downloadTemplate() {
        ExportFile file = ropaService.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    public RopaResponse get(@PathVariable long id) {
        return ropaService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RopaResponse create(Authentication authentication,
                               @Valid @RequestBody CreateRopaRequest request) {
        return ropaService.create(adminUser(authentication), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RopaResponse update(Authentication authentication,
                               @PathVariable long id,
                               @Valid @RequestBody UpdateRopaRequest request) {
        return ropaService.update(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RopaResponse assign(Authentication authentication,
                               @PathVariable long id,
                               @Valid @RequestBody AssignRopaRequest request) {
        return ropaService.assign(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/submit-review")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RopaResponse submitForReview(Authentication authentication, @PathVariable long id) {
        return ropaService.submitForReview(adminUser(authentication), id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RopaResponse approve(Authentication authentication, @PathVariable long id) {
        return ropaService.approve(adminUser(authentication), id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RopaResponse reject(Authentication authentication,
                               @PathVariable long id,
                               @RequestBody(required = false) RejectRopaRequest request) {
        return ropaService.reject(adminUser(authentication), id,
                request != null ? request : new RejectRopaRequest(null));
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }
}
