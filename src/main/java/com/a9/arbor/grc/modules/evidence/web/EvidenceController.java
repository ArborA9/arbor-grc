package com.a9.arbor.grc.modules.evidence.web;

import com.a9.arbor.grc.modules.evidence.domain.EvidenceClassification;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceStatus;
import com.a9.arbor.grc.modules.evidence.service.EvidenceExpiryScheduler;
import com.a9.arbor.grc.modules.evidence.service.EvidenceService;
import com.a9.arbor.grc.modules.evidence.web.dto.CreateEvidenceLinkRequest;
import com.a9.arbor.grc.modules.evidence.web.dto.EvidenceDetailResponse;
import com.a9.arbor.grc.modules.evidence.web.dto.EvidenceLinkResponse;
import com.a9.arbor.grc.modules.evidence.web.dto.EvidenceSummaryResponse;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Central evidence repository (req §17.2). Platform always-on: not module-licensed.
 * Vendors have no access.
 */
@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;
    private final EvidenceExpiryScheduler evidenceExpiryScheduler;
    private final UserService userService;

    public EvidenceController(EvidenceService evidenceService,
                              EvidenceExpiryScheduler evidenceExpiryScheduler,
                              UserService userService) {
        this.evidenceService = evidenceService;
        this.evidenceExpiryScheduler = evidenceExpiryScheduler;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and !hasRole('VENDOR')")
    public Page<EvidenceSummaryResponse> list(Authentication authentication,
                                              @RequestParam(required = false) String q,
                                              @RequestParam(required = false) String classification,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        User user = currentUser(authentication);
        int safe = Math.min(Math.max(size, 1), 100);
        return evidenceService.list(
                user,
                q,
                parseClassificationOptional(classification),
                parseStatusOptional(status),
                PageRequest.of(page, safe));
    }

    /** Evidence linked to a module record (req §11.5 / §17.2). */
    @GetMapping("/linked")
    @PreAuthorize("isAuthenticated() and !hasRole('VENDOR')")
    public List<EvidenceSummaryResponse> listLinked(Authentication authentication,
                                                    @RequestParam String moduleCode,
                                                    @RequestParam String recordType,
                                                    @RequestParam String recordId) {
        return evidenceService.listForRecord(currentUser(authentication), moduleCode, recordType, recordId);
    }

    /**
     * Run expiry mark + EVIDENCE_EXPIRING for this tenant (Path B4).
     * Optional {@code asOf=YYYY-MM-DD} for tests / backdated runs.
     */
    @PostMapping("/jobs/expiry-run")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Map<String, Object> runExpiry(Authentication authentication,
                                         @RequestParam(required = false) String asOf) {
        adminUser(authentication);
        LocalDate day = asOf == null || asOf.isBlank() ? LocalDate.now() : LocalDate.parse(asOf);
        var result = evidenceExpiryScheduler.runCurrentTenant(day);
        return Map.of(
                "asOf", day.toString(),
                "markedExpired", result.markedExpired(),
                "expiringNotified", result.expiringNotified());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and !hasRole('VENDOR')")
    public EvidenceDetailResponse get(Authentication authentication, @PathVariable long id) {
        return evidenceService.get(currentUser(authentication), id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public EvidenceDetailResponse upload(Authentication authentication,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam(required = false) String title,
                                         @RequestParam String classification,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiresOn,
                                         @RequestParam(required = false) String moduleCode,
                                         @RequestParam(required = false) String recordType,
                                         @RequestParam(required = false) String recordId,
                                         @RequestParam(required = false) String workflowStage) {
        return evidenceService.upload(
                currentUser(authentication),
                title,
                classification,
                expiresOn,
                file,
                moduleCode,
                recordType,
                recordId,
                workflowStage);
    }

    @PostMapping(path = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public EvidenceDetailResponse addVersion(Authentication authentication,
                                             @PathVariable long id,
                                             @RequestParam("file") MultipartFile file) {
        return evidenceService.addVersion(currentUser(authentication), id, file);
    }

    @PostMapping("/{id}/links")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public EvidenceLinkResponse addLink(Authentication authentication,
                                        @PathVariable long id,
                                        @Valid @RequestBody CreateEvidenceLinkRequest request) {
        return evidenceService.addLink(currentUser(authentication), id, request);
    }

    @DeleteMapping("/{id}/links/{linkId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public ResponseEntity<Void> removeLink(Authentication authentication,
                                           @PathVariable long id,
                                           @PathVariable long linkId) {
        evidenceService.removeLink(currentUser(authentication), id, linkId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/record-link")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public ResponseEntity<Void> unlinkFromRecord(Authentication authentication,
                                                 @PathVariable long id,
                                                 @RequestParam String moduleCode,
                                                 @RequestParam String recordType,
                                                 @RequestParam String recordId) {
        evidenceService.unlinkFromRecord(currentUser(authentication), id, moduleCode, recordType, recordId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<Void> softDelete(Authentication authentication, @PathVariable long id) {
        evidenceService.softDelete(currentUser(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated() and !hasRole('VENDOR')")
    public ResponseEntity<Resource> download(Authentication authentication,
                                             @PathVariable long id,
                                             @RequestParam(required = false) Integer version) {
        EvidenceService.Download dl = evidenceService.download(currentUser(authentication), id, version);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(dl.filename()))
                .contentType(MediaType.parseMediaType(dl.contentType()))
                .body(dl.resource());
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().noneMatch(r -> r == Role.VENDOR))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No evidence access"));
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }

    private static EvidenceClassification parseClassificationOptional(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return EvidenceClassification.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid classification", ex);
        }
    }

    private static EvidenceStatus parseStatusOptional(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return EvidenceStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status", ex);
        }
    }
}
