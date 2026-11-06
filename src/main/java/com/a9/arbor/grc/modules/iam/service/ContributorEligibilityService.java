package com.a9.arbor.grc.modules.iam.service;

import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserOrgUnit;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.UserOrgUnitRepository;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.a9.arbor.grc.modules.risk.web.dto.EligibleContributorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContributorEligibilityService {

    private final UserOrgUnitRepository userOrgUnitRepository;
    private final UserRepository userRepository;
    private final OrgUnitRepository orgUnitRepository;

    public ContributorEligibilityService(UserOrgUnitRepository userOrgUnitRepository,
                                         UserRepository userRepository,
                                         OrgUnitRepository orgUnitRepository) {
        this.userOrgUnitRepository = userOrgUnitRepository;
        this.userRepository = userRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    @Transactional(readOnly = true)
    public List<EligibleContributorResponse> listForOrgUnit(Long orgUnitId) {
        if (orgUnitId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orgUnitId is required");
        }
        if (orgUnitRepository.findById(orgUnitId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Org unit not found");
        }
        return contributorsInOrgUnits(List.of(orgUnitId));
    }

    @Transactional(readOnly = true)
    public List<EligibleContributorResponse> listForOrgUnits(List<Long> orgUnitIds) {
        if (orgUnitIds == null || orgUnitIds.isEmpty()) {
            return List.of();
        }
        for (Long id : orgUnitIds) {
            if (orgUnitRepository.findById(id).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Org unit not found: " + id);
            }
        }
        return contributorsInOrgUnits(orgUnitIds);
    }

    private List<EligibleContributorResponse> contributorsInOrgUnits(List<Long> orgUnitIds) {
        Map<Long, User> byId = new LinkedHashMap<>();
        for (Long orgUnitId : orgUnitIds) {
            for (UserOrgUnit link : userOrgUnitRepository.findByOrgUnitId(orgUnitId)) {
                userRepository.findById(link.getUserId()).ifPresent(u -> {
                    if (u.getRoles().contains(Role.CONTRIBUTOR) && u.getStatus() == UserStatus.ACTIVE) {
                        byId.putIfAbsent(u.getId(), u);
                    }
                });
            }
        }
        return byId.values().stream()
                .map(u -> new EligibleContributorResponse(u.getId(), u.getEmail(), u.getFullName()))
                .sorted(Comparator.comparing(EligibleContributorResponse::fullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EligibleContributorResponse> unionForOrgUnits(List<Long> orgUnitIds) {
        if (orgUnitIds == null || orgUnitIds.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(listForOrgUnits(orgUnitIds));
    }
}
