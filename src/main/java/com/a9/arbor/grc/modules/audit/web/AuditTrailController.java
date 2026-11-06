package com.a9.arbor.grc.modules.audit.web;

import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.audit.web.dto.AuditLogResponse;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tenant audit trail (req §8). View: Owner, Admin, Global Reader. Export: Owner/Admin.
 */
@RestController
@RequestMapping("/api/audit-trail")
public class AuditTrailController {

    private final AuditTrailService auditTrailService;
    private final UserService userService;

    public AuditTrailController(AuditTrailService auditTrailService, UserService userService) {
        this.auditTrailService = auditTrailService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER')")
    public Page<AuditLogResponse> list(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(Math.max(size, 1), 200);
        return auditTrailService.list(PageRequest.of(page, safeSize));
    }

    /** CSV export (Path B7). Optional {@code module}, {@code actionType}, {@code limit}. */
    @GetMapping("/export.csv")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<byte[]> exportCsv(Authentication authentication,
                                            @RequestParam(required = false) String module,
                                            @RequestParam(required = false) String actionType,
                                            @RequestParam(required = false) Integer limit) {
        User user = userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Export not permitted"));
        var file = auditTrailService.exportCsv(user, module, actionType, limit);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }
}
