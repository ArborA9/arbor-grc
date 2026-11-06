package com.a9.arbor.grc.modules.capa.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.capa.service.CapaRegisterService;
import com.a9.arbor.grc.modules.capa.web.dto.CapaAvailableFindingResponse;
import com.a9.arbor.grc.modules.capa.web.dto.CapaRegisterResponse;
import com.a9.arbor.grc.modules.capa.web.dto.CreateCapaRegisterRequest;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.security.RequiresModule;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/capa/registers")
@RequiresModule({ModuleCode.ISMS, ModuleCode.AUDIT, ModuleCode.BCMS, ModuleCode.ERM})
public class CapaRegisterController {

    private final CapaRegisterService capaRegisterService;
    private final UserService userService;

    public CapaRegisterController(CapaRegisterService capaRegisterService, UserService userService) {
        this.capaRegisterService = capaRegisterService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER','CONTRIBUTOR')")
    public List<CapaRegisterResponse> list(Authentication authentication) {
        return capaRegisterService.list(current(authentication));
    }

    @GetMapping("/available-findings")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER','CONTRIBUTOR')")
    public List<CapaAvailableFindingResponse> availableFindings(Authentication authentication) {
        return capaRegisterService.listAvailableFindings(current(authentication));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public CapaRegisterResponse create(Authentication authentication,
                                       @Valid @RequestBody CreateCapaRegisterRequest request) {
        return capaRegisterService.create(current(authentication), request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER','CONTRIBUTOR')")
    public CapaRegisterResponse get(Authentication authentication, @PathVariable long id) {
        return capaRegisterService.get(current(authentication), id);
    }

    private User current(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
