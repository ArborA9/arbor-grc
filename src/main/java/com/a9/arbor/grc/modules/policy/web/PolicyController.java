package com.a9.arbor.grc.modules.policy.web;

import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.policy.domain.Policy;
import com.a9.arbor.grc.modules.policy.domain.PolicyCategory;
import com.a9.arbor.grc.modules.policy.domain.PolicyStatus;
import com.a9.arbor.grc.modules.policy.service.PolicyService;
import com.a9.arbor.grc.modules.policy.web.dto.CreatePolicyRequest;
import com.a9.arbor.grc.modules.policy.web.dto.PolicyAcknowledgementResponse;
import com.a9.arbor.grc.modules.policy.web.dto.PolicyDetailResponse;
import com.a9.arbor.grc.modules.policy.web.dto.PolicySummaryResponse;
import com.a9.arbor.grc.modules.policy.web.dto.PolicyVersionResponse;
import com.a9.arbor.grc.modules.policy.web.dto.RejectPolicyRequest;
import com.a9.arbor.grc.modules.policy.web.dto.UpdatePolicyRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Platform always-on Policy Repository (req §17.3). */
@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;
    private final UserService userService;

    public PolicyController(PolicyService policyService, UserService userService) {
        this.policyService = policyService;
        this.userService = userService;
    }

    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated() and !hasRole('VENDOR')")
    public List<PolicyCategory> categories() {
        return policyService.categoriesForTenant();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public List<PolicySummaryResponse> list(@RequestParam(required = false) PolicyCategory category,
                                            @RequestParam(required = false) PolicyStatus status) {
        return policyService.list(category, status);
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public PolicyDetailResponse get(Authentication authentication, @PathVariable long id) {
        return policyService.get(currentUser(authentication), id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public PolicyDetailResponse create(Authentication authentication,
                                       @Valid @RequestBody CreatePolicyRequest request) {
        return policyService.create(adminUser(authentication), request);
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public PolicyDetailResponse update(Authentication authentication,
                                       @PathVariable long id,
                                       @Valid @RequestBody UpdatePolicyRequest request) {
        return policyService.update(adminUser(authentication), id, request);
    }

    @PostMapping(value = "/{id:\\d+}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public PolicyDetailResponse uploadFile(Authentication authentication,
                                           @PathVariable long id,
                                           @RequestParam("file") MultipartFile file) {
        return policyService.uploadFile(adminUser(authentication), id, file);
    }

    @GetMapping("/{id:\\d+}/file")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public ResponseEntity<Resource> downloadFile(Authentication authentication, @PathVariable long id) {
        User user = currentUser(authentication);
        Policy p = policyService.require(id);
        Resource body = policyService.downloadFile(user, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(policyService.downloadFilename(p)))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    @PostMapping("/{id:\\d+}/submit")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public PolicyDetailResponse submit(Authentication authentication, @PathVariable long id) {
        return policyService.submit(adminUser(authentication), id);
    }

    @PostMapping("/{id:\\d+}/recommend")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public PolicyDetailResponse recommend(Authentication authentication, @PathVariable long id) {
        return policyService.recommend(adminUser(authentication), id);
    }

    @PostMapping("/{id:\\d+}/reject")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public PolicyDetailResponse reject(Authentication authentication,
                                       @PathVariable long id,
                                       @Valid @RequestBody RejectPolicyRequest request) {
        return policyService.reject(adminUser(authentication), id, request);
    }

    @PostMapping("/{id:\\d+}/approve")
    @PreAuthorize("hasRole('OWNER')")
    public PolicyDetailResponse approve(Authentication authentication, @PathVariable long id) {
        return policyService.approve(currentUser(authentication), id);
    }

    @PostMapping("/{id:\\d+}/acknowledge")
    @PreAuthorize("isAuthenticated() and !hasRole('VENDOR')")
    public PolicyDetailResponse acknowledge(Authentication authentication, @PathVariable long id) {
        return policyService.acknowledge(currentUser(authentication), id);
    }

    @GetMapping("/{id:\\d+}/versions")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public List<PolicyVersionResponse> versions(@PathVariable long id) {
        return policyService.listVersions(id);
    }

    @GetMapping("/{id:\\d+}/acknowledgements/export")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR')")
    public ResponseEntity<byte[]> exportAcknowledgements(@PathVariable long id) {
        byte[] body = policyService.exportAcknowledgementsCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDispositionSupport.attachment("policy-" + id + "-acks.csv"))
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    @GetMapping("/{id:\\d+}/acknowledgements")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR')")
    public List<PolicyAcknowledgementResponse> acknowledgements(@PathVariable long id) {
        return policyService.listAcknowledgements(id);
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }
}
