package com.a9.arbor.grc.modules.dashboard.web;

import com.a9.arbor.grc.modules.dashboard.service.ReportScheduleService;
import com.a9.arbor.grc.modules.dashboard.web.dto.CreateReportScheduleRequest;
import com.a9.arbor.grc.modules.dashboard.web.dto.ReportDeliveryResponse;
import com.a9.arbor.grc.modules.dashboard.web.dto.ReportScheduleResponse;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import jakarta.validation.Valid;
import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/report-schedules")
public class ReportScheduleController {

    private final ReportScheduleService reportScheduleService;
    private final UserService userService;

    public ReportScheduleController(ReportScheduleService reportScheduleService, UserService userService) {
        this.reportScheduleService = reportScheduleService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<ReportScheduleResponse> list() {
        return reportScheduleService.listSchedules();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ReportScheduleResponse create(Authentication authentication,
                                         @Valid @RequestBody CreateReportScheduleRequest req) {
        return reportScheduleService.create(requireAdmin(authentication), req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void deactivate(Authentication authentication, @PathVariable long id) {
        reportScheduleService.deactivate(requireAdmin(authentication), id);
    }

    @GetMapping("/deliveries")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<ReportDeliveryResponse> listDeliveries(Authentication authentication) {
        return reportScheduleService.listDeliveries(requireUser(authentication));
    }

    @GetMapping("/deliveries/{id}/download")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<byte[]> downloadDelivery(Authentication authentication, @PathVariable long id) {
        ExportFile file = reportScheduleService.downloadDelivery(requireUser(authentication), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    private User requireAdmin(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only"));
    }

    private User requireUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }
}
