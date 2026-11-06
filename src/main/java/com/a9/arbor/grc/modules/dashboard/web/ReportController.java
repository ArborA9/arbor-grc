package com.a9.arbor.grc.modules.dashboard.web;

import com.a9.arbor.grc.modules.dashboard.service.ReportService;
import com.a9.arbor.grc.modules.dashboard.web.dto.ReportDescriptor;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Reports hub (req §19.3): 6 key reports for Sprint 8. Global Reader can list (view) but not
 * export; Contributor / Vendor cannot access reports at all (req §19.3 report standards).
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    public ReportController(ReportService reportService, UserService userService) {
        this.reportService = reportService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER')")
    public List<ReportDescriptor> list() {
        return reportService.list();
    }

    @GetMapping("/{id}/export")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<byte[]> export(Authentication authentication,
                                         @PathVariable String id,
                                         @RequestParam(defaultValue = "xlsx") String format,
                                         @RequestParam(required = false) Long soaId,
                                         @RequestParam(required = false) Long gapId,
                                         @RequestParam(required = false) Long auditId,
                                         @RequestParam(required = false) Long exerciseId,
                                         @RequestParam(required = false) String variant) {
        User user = exporterUser(authentication);
        ExportFile file = reportService.export(user, id, format, soaId, gapId, auditId, exerciseId, variant);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    private User exporterUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r ->
                        r == Role.OWNER || r == Role.ADMIN || r == Role.INTERNAL_AUDITOR || r == Role.EXTERNAL_AUDITOR))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Export not permitted"));
    }
}
