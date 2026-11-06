package com.a9.arbor.grc.modules.tprm.web;

import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.iam.web.dto.LoginResponse;
import com.a9.arbor.grc.modules.tprm.service.TprmVendorAccessService;
import com.a9.arbor.grc.modules.tprm.web.dto.AssessmentResponse;
import com.a9.arbor.grc.modules.tprm.web.dto.RedeemVendorAccessRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.RequestVendorOtpRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.RequestVendorOtpResponse;
import com.a9.arbor.grc.modules.tprm.web.dto.SaveAnswersRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.ValidateVendorAccessResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Public vendor portal endpoints: {@code permitAll} in SecurityConfig (see /api/tprm/vendor-access/**). */
@RestController
@RequestMapping("/api/tprm/vendor-access")
public class TprmVendorAccessController {

    private final TprmVendorAccessService accessService;
    private final UserService userService;

    public TprmVendorAccessController(TprmVendorAccessService accessService, UserService userService) {
        this.accessService = accessService;
        this.userService = userService;
    }

    @GetMapping("/validate")
    public ValidateVendorAccessResponse validate(@RequestParam String tenantId, @RequestParam String token) {
        return accessService.validate(tenantId, token);
    }

    @PostMapping("/request-otp")
    public RequestVendorOtpResponse requestOtp(@Valid @RequestBody RequestVendorOtpRequest request) {
        return accessService.requestOtp(request.tenantId(), request.token(), request.email());
    }

    @PostMapping("/redeem")
    public LoginResponse redeem(@Valid @RequestBody RedeemVendorAccessRequest request) {
        return accessService.redeem(request.tenantId(), request.token(), request.otp());
    }

    @GetMapping("/assessment")
    @PreAuthorize("hasRole('VENDOR')")
    public AssessmentResponse assessment(Authentication authentication) {
        return accessService.getAssessmentForCurrentVendor(vendorUser(authentication));
    }

    @PutMapping("/answers")
    @PreAuthorize("hasRole('VENDOR')")
    public AssessmentResponse saveAnswers(Authentication authentication, @Valid @RequestBody SaveAnswersRequest request) {
        return accessService.saveAnswers(vendorUser(authentication), request);
    }

    @PostMapping(value = "/answers/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('VENDOR')")
    public AssessmentResponse uploadAnswerEvidence(Authentication authentication,
                                                   @RequestParam long itemId,
                                                   @RequestParam("file") MultipartFile file) {
        return accessService.uploadAnswerEvidence(vendorUser(authentication), itemId, file);
    }

    @DeleteMapping("/answers/evidence")
    @PreAuthorize("hasRole('VENDOR')")
    public AssessmentResponse removeAnswerEvidence(Authentication authentication,
                                                   @RequestParam long itemId,
                                                   @RequestParam long evidenceId) {
        return accessService.removeAnswerEvidence(vendorUser(authentication), itemId, evidenceId);
    }

    @GetMapping("/answers/evidence/{evidenceId}/file")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<Resource> downloadAnswerEvidence(Authentication authentication,
                                                           @RequestParam long itemId,
                                                           @PathVariable long evidenceId) {
        var dl = accessService.downloadAnswerEvidence(vendorUser(authentication), itemId, evidenceId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(dl.filename()))
                .contentType(MediaType.parseMediaType(
                        dl.contentType() != null ? dl.contentType() : "application/octet-stream"))
                .body(dl.resource());
    }

    @GetMapping("/questionnaire.xlsx")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<byte[]> downloadExcel(Authentication authentication) {
        var file = accessService.downloadExcel(vendorUser(authentication));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @PostMapping(value = "/questionnaire.xlsx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('VENDOR')")
    public AssessmentResponse uploadExcel(Authentication authentication, @RequestParam("file") MultipartFile file) {
        return accessService.uploadExcel(vendorUser(authentication), file);
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('VENDOR')")
    public AssessmentResponse submit(Authentication authentication) {
        return accessService.submit(vendorUser(authentication));
    }

    private User vendorUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
