package com.a9.arbor.grc.modules.license.web;

import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.license.service.LicenseBannerService;
import com.a9.arbor.grc.modules.license.web.dto.DismissLicenseBannerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/license")
public class LicenseBannerController {

    private final LicenseBannerService licenseBannerService;
    private final UserService userService;

    public LicenseBannerController(LicenseBannerService licenseBannerService, UserService userService) {
        this.licenseBannerService = licenseBannerService;
        this.userService = userService;
    }

    @PostMapping("/banner-dismiss")
    public Map<String, String> dismiss(Authentication authentication,
                                       @Valid @RequestBody DismissLicenseBannerRequest request) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not signed in"));
        licenseBannerService.dismiss(user.getId(), request.dismissKey());
        return Map.of("status", "ok");
    }
}
