package com.a9.arbor.grc.modules.privacy.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.privacy.service.PrivacyDpiaService;
import com.a9.arbor.grc.modules.privacy.web.dto.AssignDpiaRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateDpiaRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.DpiaResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.RejectDpiaRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.UpdateDpiaRequest;
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
@RequestMapping("/api/privacy/dpia")
@RequiresModule(ModuleCode.PRIVACY)
public class PrivacyDpiaController {

    private static final String READ_ROLES =
            "hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')";

    private final PrivacyDpiaService dpiaService;
    private final UserService userService;

    public PrivacyDpiaController(PrivacyDpiaService dpiaService, UserService userService) {
        this.dpiaService = dpiaService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize(READ_ROLES)
    public List<DpiaResponse> list() {
        return dpiaService.list();
    }

    @GetMapping("/methodology-bank")
    @PreAuthorize(READ_ROLES)
    public Object methodologyBank() {
        return dpiaService.methodologyBank();
    }

    @GetMapping("/template.xlsx")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public ResponseEntity<byte[]> downloadTemplate() {
        ExportFile file = dpiaService.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    public DpiaResponse get(@PathVariable long id) {
        return dpiaService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public DpiaResponse create(Authentication authentication, @Valid @RequestBody CreateDpiaRequest request) {
        return dpiaService.create(adminUser(authentication), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public DpiaResponse update(Authentication authentication,
                               @PathVariable long id,
                               @Valid @RequestBody UpdateDpiaRequest request) {
        return dpiaService.update(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public DpiaResponse assign(Authentication authentication,
                               @PathVariable long id,
                               @Valid @RequestBody AssignDpiaRequest request) {
        return dpiaService.assign(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/submit-review")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public DpiaResponse submitForReview(Authentication authentication, @PathVariable long id) {
        return dpiaService.submitForReview(adminUser(authentication), id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public DpiaResponse approve(Authentication authentication, @PathVariable long id) {
        return dpiaService.approve(adminUser(authentication), id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public DpiaResponse reject(Authentication authentication,
                               @PathVariable long id,
                               @Valid @RequestBody RejectDpiaRequest request) {
        return dpiaService.reject(adminUser(authentication), id, request);
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }
}
