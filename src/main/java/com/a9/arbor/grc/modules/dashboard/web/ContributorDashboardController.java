package com.a9.arbor.grc.modules.dashboard.web;

import com.a9.arbor.grc.modules.dashboard.service.ContributorDashboardService;
import com.a9.arbor.grc.modules.dashboard.web.dto.ContributorDashboardResponse;
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
 * Contributor-only workload dashboard. Not {@code GET /api/dashboard/summary}
 * (Admin / Owner / Reader / Auditor org posture).
 */
@RestController
@RequestMapping("/api/dashboard/contributor")
public class ContributorDashboardController {

    private final ContributorDashboardService contributorDashboardService;
    private final UserService userService;

    public ContributorDashboardController(
            ContributorDashboardService contributorDashboardService,
            UserService userService) {
        this.contributorDashboardService = contributorDashboardService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CONTRIBUTOR')")
    public ContributorDashboardResponse summary(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown user"));
        return contributorDashboardService.summary(user);
    }
}
