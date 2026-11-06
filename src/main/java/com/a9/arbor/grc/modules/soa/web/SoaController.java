package com.a9.arbor.grc.modules.soa.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.soa.service.SoaService;
import com.a9.arbor.grc.modules.soa.web.dto.BulkUpdateSoaItemsRequest;
import com.a9.arbor.grc.modules.soa.web.dto.CreateSoaRequest;
import com.a9.arbor.grc.modules.soa.web.dto.SoaDetailResponse;
import com.a9.arbor.grc.modules.soa.web.dto.SoaItemResponse;
import com.a9.arbor.grc.modules.soa.web.dto.SoaSummaryResponse;
import com.a9.arbor.grc.modules.soa.web.dto.UpdateSoaItemRequest;
import com.a9.arbor.grc.security.RequiresModule;
import jakarta.validation.Valid;
import com.a9.arbor.grc.common.web.ContentDispositionSupport;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/soa")
@RequiresModule({
        ModuleCode.ISMS, ModuleCode.BCMS, ModuleCode.QMS, ModuleCode.OHSMS, ModuleCode.SAMA,
        ModuleCode.ITSM, ModuleCode.AIMS, ModuleCode.PRIVACY
})
public class SoaController {

    private final SoaService soaService;
    private final UserService userService;

    public SoaController(SoaService soaService, UserService userService) {
        this.soaService = soaService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<SoaSummaryResponse> list(@RequestParam(required = false) Long projectId) {
        return soaService.list(projectId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public SoaDetailResponse get(@PathVariable long id) {
        return soaService.get(id);
    }

    /** Excel export: COMPLETE SoA only. Contributor / Global Reader cannot export. */
    @GetMapping("/{id}/export.xlsx")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<byte[]> exportExcel(Authentication authentication, @PathVariable long id) {
        var file = soaService.exportExcel(exporterUser(authentication), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    /** PDF export: COMPLETE SoA only (§19.3). */
    @GetMapping("/{id}/export.pdf")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<byte[]> exportPdf(Authentication authentication, @PathVariable long id) {
        var file = soaService.exportPdf(exporterUser(authentication), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public SoaDetailResponse create(Authentication authentication,
                                    @Valid @RequestBody CreateSoaRequest request) {
        return soaService.create(adminUser(authentication), request);
    }

    @PutMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public SoaItemResponse updateItem(Authentication authentication,
                                      @PathVariable long id,
                                      @PathVariable long itemId,
                                      @Valid @RequestBody UpdateSoaItemRequest request) {
        return soaService.updateItem(adminUser(authentication), id, itemId, request);
    }

    @PostMapping("/{id}/items/bulk")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public SoaDetailResponse bulkUpdateItems(Authentication authentication,
                                             @PathVariable long id,
                                             @Valid @RequestBody BulkUpdateSoaItemsRequest request) {
        return soaService.bulkUpdateItems(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public SoaDetailResponse complete(Authentication authentication, @PathVariable long id) {
        return soaService.complete(adminUser(authentication), id);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public SoaDetailResponse reopen(Authentication authentication, @PathVariable long id) {
        return soaService.reopen(adminUser(authentication), id);
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }

    private User exporterUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r ->
                        r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Export not permitted"));
    }
}
