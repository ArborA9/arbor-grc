package com.a9.arbor.grc.modules.dashboard.web;

import com.a9.arbor.grc.modules.dashboard.service.DashboardService;
import com.a9.arbor.grc.modules.dashboard.web.dto.DashboardSummaryResponse;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Overview dashboard (req §19.1). Platform always-on: no {@code @RequiresModule} gate here;
 * individual widgets check module licensing internally. Contributor / Vendor are excluded
 * (403). Contributors use {@code GET /api/dashboard/contributor} instead.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    public DashboardController(DashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public DashboardSummaryResponse summary(Authentication authentication) {
        User user = currentUser(authentication);
        return dashboardService.summary(user);
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown user"));
    }
}
