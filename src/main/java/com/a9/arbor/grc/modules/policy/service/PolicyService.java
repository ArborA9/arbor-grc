package com.a9.arbor.grc.modules.policy.service;

import com.a9.arbor.grc.common.validation.DateRules;
import com.a9.arbor.grc.catalog.domain.Framework;
import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.service.FrameworkQueryService;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
import com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserOrgUnit;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.UserOrgUnitRepository;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.policy.domain.Policy;
import com.a9.arbor.grc.modules.policy.domain.PolicyAckOrgUnit;
import com.a9.arbor.grc.modules.policy.domain.PolicyAckScopeType;
import com.a9.arbor.grc.modules.policy.domain.PolicyAckUser;
import com.a9.arbor.grc.modules.policy.domain.PolicyAcknowledgement;
import com.a9.arbor.grc.modules.policy.domain.PolicyCategory;
import com.a9.arbor.grc.modules.policy.domain.PolicyFramework;
import com.a9.arbor.grc.modules.policy.domain.PolicyStatus;
import com.a9.arbor.grc.modules.policy.domain.PolicyVersion;
import com.a9.arbor.grc.modules.policy.repo.PolicyAckOrgUnitRepository;
import com.a9.arbor.grc.modules.policy.repo.PolicyAckUserRepository;
import com.a9.arbor.grc.modules.policy.repo.PolicyAcknowledgementRepository;
import com.a9.arbor.grc.modules.policy.repo.PolicyFrameworkRepository;
import com.a9.arbor.grc.modules.policy.repo.PolicyRepository;
import com.a9.arbor.grc.modules.policy.repo.PolicyVersionRepository;
import com.a9.arbor.grc.modules.policy.web.dto.CreatePolicyRequest;
import com.a9.arbor.grc.modules.policy.web.dto.PolicyAcknowledgementResponse;
import com.a9.arbor.grc.modules.policy.web.dto.PolicyDetailResponse;
import com.a9.arbor.grc.modules.policy.web.dto.PolicyFrameworkResponse;
import com.a9.arbor.grc.modules.policy.web.dto.PolicySummaryResponse;
import com.a9.arbor.grc.modules.policy.web.dto.PolicyTaskResponse;
import com.a9.arbor.grc.modules.policy.web.dto.PolicyVersionResponse;
import com.a9.arbor.grc.modules.policy.web.dto.RejectPolicyRequest;
import com.a9.arbor.grc.modules.policy.web.dto.UpdatePolicyRequest;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import com.a9.arbor.grc.modules.task.service.MyTasksService;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyVersionRepository versionRepository;
    private final PolicyFrameworkRepository frameworkRepository;
    private final PolicyAckOrgUnitRepository ackOrgUnitRepository;
    private final PolicyAckUserRepository ackUserRepository;
    private final PolicyAcknowledgementRepository acknowledgementRepository;
    private final UserRepository userRepository;
    private final UserOrgUnitRepository userOrgUnitRepository;
    private final WorkTaskRepository workTaskRepository;
    private final FrameworkQueryService frameworkQueryService;
    private final ModuleLicensingService moduleLicensingService;
    private final AdminModuleAccessService adminModuleAccessService;
    private final PolicyFileStore policyFileStore;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;

    public PolicyService(PolicyRepository policyRepository,
                         PolicyVersionRepository versionRepository,
                         PolicyFrameworkRepository frameworkRepository,
                         PolicyAckOrgUnitRepository ackOrgUnitRepository,
                         PolicyAckUserRepository ackUserRepository,
                         PolicyAcknowledgementRepository acknowledgementRepository,
                         UserRepository userRepository,
                         UserOrgUnitRepository userOrgUnitRepository,
                         WorkTaskRepository workTaskRepository,
                         FrameworkQueryService frameworkQueryService,
                         ModuleLicensingService moduleLicensingService,
                         AdminModuleAccessService adminModuleAccessService,
                         PolicyFileStore policyFileStore,
                         NotificationService notificationService,
                         AuditTrailService auditTrailService) {
        this.policyRepository = policyRepository;
        this.versionRepository = versionRepository;
        this.frameworkRepository = frameworkRepository;
        this.ackOrgUnitRepository = ackOrgUnitRepository;
        this.ackUserRepository = ackUserRepository;
        this.acknowledgementRepository = acknowledgementRepository;
        this.userRepository = userRepository;
        this.userOrgUnitRepository = userOrgUnitRepository;
        this.workTaskRepository = workTaskRepository;
        this.frameworkQueryService = frameworkQueryService;
        this.moduleLicensingService = moduleLicensingService;
        this.adminModuleAccessService = adminModuleAccessService;
        this.policyFileStore = policyFileStore;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<PolicyCategory> categoriesForTenant() {
        String tenantId = TenantContext.getTenantId();
        List<PolicyCategory> out = new ArrayList<>();
        out.add(PolicyCategory.GENERAL_HR);
        if (tenantId != null) {
            if (policyCategoryVisible(tenantId, ModuleCode.ISMS, false)) out.add(PolicyCategory.ISMS);
            if (policyCategoryVisible(tenantId, ModuleCode.BCMS, false)) out.add(PolicyCategory.BCMS);
            if (policyCategoryVisible(tenantId, ModuleCode.ITSM, false)) out.add(PolicyCategory.ITSM);
            if (policyCategoryVisible(tenantId, ModuleCode.PRIVACY, false)) out.add(PolicyCategory.PRIVACY);
            if (policyCategoryVisible(tenantId, ModuleCode.ERM, false)) out.add(PolicyCategory.ERM);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<PolicySummaryResponse> list(PolicyCategory category, PolicyStatus status) {
        List<Policy> rows;
        if (category != null) {
            rows = policyRepository.findByCategoryOrderByUpdatedAtDesc(category);
        } else if (status != null) {
            rows = policyRepository.findByStatusOrderByUpdatedAtDesc(status);
        } else {
            rows = policyRepository.findAllByOrderByUpdatedAtDesc();
        }
        java.util.Set<PolicyCategory> allowed = new java.util.HashSet<>(categoriesForTenant());
        return rows.stream()
                .filter(p -> allowed.contains(p.getCategory()))
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PolicyDetailResponse get(User viewer, long id) {
        Policy p = require(id);
        if (!categoriesForTenant().contains(p.getCategory())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found");
        }
        return toDetail(viewer, p);
    }

    @Transactional
    public PolicyDetailResponse create(User admin, CreatePolicyRequest req) {
        assertAdmin(admin);
        assertCategoryAllowed(req.category());
        Policy p = new Policy();
        p.setTitle(req.title().trim());
        p.setCategory(req.category());
        p.setStatus(PolicyStatus.DRAFT);
        p.setOrgUnitId(req.orgUnitId());
        p.setBodyDraft(blankToNull(req.bodyText()));
        DateRules.requireActionDueDateIfPresent(req.reviewDate(), "Review date");
        p.setReviewDate(req.reviewDate());
        boolean ackRequired = req.ackRequired() == null || req.ackRequired();
        p.setAckRequired(ackRequired);
        p.setAckScopeType(req.ackScopeType() != null ? req.ackScopeType() : PolicyAckScopeType.ALL_ACTIVE_USERS);
        DateRules.requireActionDueDateIfPresent(ackRequired ? req.ackDeadline() : null, "Acknowledgement deadline");
        p.setAckDeadline(req.ackDeadline());
        p.setOwnerUserId(req.ownerUserId());
        p.setCreatedByUserId(admin.getId());
        policyRepository.save(p);
        replaceFrameworks(p.getId(), req.frameworkIds());
        replaceAckScope(p, req.ackOrgUnitIds(), req.ackUserIds());
        audit(admin, AuditActionType.CREATED, p);
        return toDetail(admin, p);
    }

    @Transactional
    public PolicyDetailResponse update(User admin, long id, UpdatePolicyRequest req) {
        assertAdmin(admin);
        Policy p = require(id);
        if (p.getStatus() != PolicyStatus.DRAFT && p.getStatus() != PolicyStatus.DUE_FOR_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only DRAFT or DUE_FOR_REVIEW policies can be edited");
        }
        assertCategoryAllowed(req.category());
        if (p.getStatus() == PolicyStatus.DUE_FOR_REVIEW) {
            p.setStatus(PolicyStatus.DRAFT);
            p.setRejectReason(null);
            p.setReviewDueNotifiedAt(null);
            p.setExpiredNotifiedAt(null);
            p.setAckDeadlineNotifiedAt(null);
            p.setAckOverdueNotifiedAt(null);
        }
        p.setTitle(req.title().trim());
        p.setCategory(req.category());
        p.setOrgUnitId(req.orgUnitId());
        p.setBodyDraft(blankToNull(req.bodyText()));
        DateRules.requireActionDueDateIfChanged(p.getReviewDate(), req.reviewDate(), "Review date");
        p.setReviewDate(req.reviewDate());
        if (req.ackRequired() != null) {
            p.setAckRequired(req.ackRequired());
        }
        if (req.ackScopeType() != null) {
            p.setAckScopeType(req.ackScopeType());
        }
        boolean ackRequired = req.ackRequired() != null ? req.ackRequired() : p.isAckRequired();
        DateRules.requireActionDueDateIfChanged(
                p.getAckDeadline(),
                ackRequired ? req.ackDeadline() : null,
                "Acknowledgement deadline");
        p.setAckDeadline(req.ackDeadline());
        p.setOwnerUserId(req.ownerUserId());
        policyRepository.save(p);
        replaceFrameworks(p.getId(), req.frameworkIds());
        replaceAckScope(p, req.ackOrgUnitIds(), req.ackUserIds());
        audit(admin, AuditActionType.UPDATED, p);
        return toDetail(admin, p);
    }

    @Transactional
    public PolicyDetailResponse uploadFile(User admin, long id, MultipartFile file) {
        assertAdmin(admin);
        Policy p = require(id);
        if (p.getStatus() != PolicyStatus.DRAFT && p.getStatus() != PolicyStatus.DUE_FOR_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "File upload only on editable policies");
        }
        if (p.getStatus() == PolicyStatus.DUE_FOR_REVIEW) {
            p.setStatus(PolicyStatus.DRAFT);
        }
        PolicyFileStore.StoredFile stored = policyFileStore.store(p.getId(), file);
        p.setStorageKeyDraft(stored.storageKey());
        p.setOriginalFilenameDraft(stored.originalFilename());
        p.setContentTypeDraft(stored.contentType());
        p.setSizeBytesDraft(stored.sizeBytes());
        policyRepository.save(p);
        return toDetail(admin, p);
    }

    @Transactional(readOnly = true)
    public Resource downloadFile(User viewer, long id) {
        Policy p = require(id);
        if (p.getCurrentVersionId() != null) {
            PolicyVersion v = versionRepository.findById(p.getCurrentVersionId()).orElse(null);
            if (v != null && v.getStorageKey() != null) {
                return policyFileStore.load(v.getStorageKey());
            }
        }
        if (p.getStorageKeyDraft() != null) {
            return policyFileStore.load(p.getStorageKeyDraft());
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No file attached");
    }

    @Transactional(readOnly = true)
    public String downloadFilename(Policy p) {
        if (p.getCurrentVersionId() != null) {
            return versionRepository.findById(p.getCurrentVersionId())
                    .map(PolicyVersion::getOriginalFilename)
                    .filter(Objects::nonNull)
                    .orElse(p.getOriginalFilenameDraft() != null ? p.getOriginalFilenameDraft() : "policy.bin");
        }
        return p.getOriginalFilenameDraft() != null ? p.getOriginalFilenameDraft() : "policy.bin";
    }

    @Transactional
    public PolicyDetailResponse submit(User admin, long id) {
        assertAdmin(admin);
        Policy p = require(id);
        if (p.getStatus() != PolicyStatus.DRAFT && p.getStatus() != PolicyStatus.DUE_FOR_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only DRAFT/DUE_FOR_REVIEW can be submitted");
        }
        if ((p.getBodyDraft() == null || p.getBodyDraft().isBlank()) && p.getStorageKeyDraft() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add policy text or a file before submit");
        }
        p.setStatus(PolicyStatus.UNDER_REVIEW);
        p.setRejectReason(null);
        policyRepository.save(p);
        notificationService.trigger("POLICY_SUBMITTED", String.valueOf(p.getId()),
                List.of("OWNER", "ADMIN"), baseData(p));
        audit(admin, AuditActionType.UPDATED, p);
        return toDetail(admin, p);
    }

    @Transactional
    public PolicyDetailResponse recommend(User admin, long id) {
        assertAdmin(admin);
        Policy p = require(id);
        if (p.getStatus() != PolicyStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only UNDER_REVIEW can be recommended");
        }
        p.setStatus(PolicyStatus.PENDING_OWNER);
        policyRepository.save(p);
        notificationService.trigger("POLICY_SUBMITTED", String.valueOf(p.getId()),
                List.of("OWNER"), baseData(p));
        audit(admin, AuditActionType.UPDATED, p);
        return toDetail(admin, p);
    }

    @Transactional
    public PolicyDetailResponse reject(User admin, long id, RejectPolicyRequest req) {
        assertAdmin(admin);
        Policy p = require(id);
        if (p.getStatus() != PolicyStatus.UNDER_REVIEW && p.getStatus() != PolicyStatus.PENDING_OWNER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only UNDER_REVIEW/PENDING_OWNER can be rejected");
        }
        p.setStatus(PolicyStatus.DRAFT);
        p.setRejectReason(req.reason().trim());
        policyRepository.save(p);
        audit(admin, AuditActionType.REJECTED, p);
        return toDetail(admin, p);
    }

    @Transactional
    public PolicyDetailResponse approve(User owner, long id) {
        if (!owner.getRoles().contains(Role.OWNER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner approval required");
        }
        Policy p = require(id);
        if (p.getStatus() != PolicyStatus.PENDING_OWNER && p.getStatus() != PolicyStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only UNDER_REVIEW or PENDING_OWNER policies can be approved by Owner");
        }
        int nextMajor = p.getMajorVersion() <= 0 ? 1 : p.getMajorVersion() + 1;
        PolicyVersion ver = new PolicyVersion();
        ver.setPolicyId(p.getId());
        ver.setMajorVersion(nextMajor);
        ver.setMinorVersion(0);
        ver.setBodyText(p.getBodyDraft());
        ver.setStorageKey(p.getStorageKeyDraft());
        ver.setOriginalFilename(p.getOriginalFilenameDraft());
        ver.setContentType(p.getContentTypeDraft());
        ver.setSizeBytes(p.getSizeBytesDraft());
        ver.setApprovedAt(Instant.now());
        ver.setApprovedByUserId(owner.getId());
        ver.setCreatedByUserId(owner.getId());
        versionRepository.save(ver);

        p.setMajorVersion(nextMajor);
        p.setMinorVersion(0);
        p.setCurrentVersionId(ver.getId());
        p.setStatus(PolicyStatus.APPROVED);
        p.setRejectReason(null);
        p.setOwnerUserId(owner.getId());
        p.setAckDeadlineNotifiedAt(null);
        p.setAckOverdueNotifiedAt(null);
        policyRepository.save(p);

        Map<String, String> data = baseData(p);
        data.put("version", "v" + nextMajor + ".0");
        notificationService.trigger("POLICY_APPROVED", String.valueOf(p.getId()),
                List.of("OWNER", "ADMIN"), data);

        if (p.isAckRequired()) {
            spawnAckTasks(owner, p, ver);
        }
        audit(owner, AuditActionType.APPROVED, p);
        return toDetail(owner, p);
    }

    @Transactional
    public PolicyDetailResponse acknowledge(User user, long id) {
        Policy p = require(id);
        if (p.getStatus() != PolicyStatus.APPROVED && p.getStatus() != PolicyStatus.DUE_FOR_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Policy is not approved for acknowledgement");
        }
        if (!p.isAckRequired() || p.getCurrentVersionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Acknowledgement not required");
        }
        if (!isInAckScope(p, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not in acknowledgement scope");
        }
        if (acknowledgementRepository.existsByPolicyVersionIdAndUserId(p.getCurrentVersionId(), user.getId())) {
            return toDetail(user, p);
        }
        PolicyAcknowledgement ack = new PolicyAcknowledgement();
        ack.setPolicyVersionId(p.getCurrentVersionId());
        ack.setUserId(user.getId());
        ack.setAcknowledgedAt(Instant.now());
        acknowledgementRepository.save(ack);

        workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordIdAndAssignedToUserId(
                        MyTasksService.MODULE_POLICY, MyTasksService.TYPE_POLICY_ACK, p.getId(), user.getId())
                .ifPresent(t -> {
                    t.setStatus(WorkTaskStatus.SUBMITTED);
                    t.setSubmittedAt(Instant.now());
                    workTaskRepository.save(t);
                });

        return toDetail(user, p);
    }

    @Transactional(readOnly = true)
    public List<PolicyVersionResponse> listVersions(long id) {
        require(id);
        return versionRepository.findByPolicyIdOrderByMajorVersionDescMinorVersionDesc(id).stream()
                .map(this::toVersion)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PolicyAcknowledgementResponse> listAcknowledgements(long id) {
        Policy p = require(id);
        if (p.getCurrentVersionId() == null) {
            return List.of();
        }
        return acknowledgementRepository.findByPolicyVersionIdOrderByAcknowledgedAtAsc(p.getCurrentVersionId())
                .stream()
                .map(a -> {
                    User u = userRepository.findById(a.getUserId()).orElse(null);
                    return new PolicyAcknowledgementResponse(
                            a.getId(),
                            a.getPolicyVersionId(),
                            a.getUserId(),
                            u != null ? u.getEmail() : null,
                            u != null ? u.getFullName() : null,
                            a.getAcknowledgedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportAcknowledgementsCsv(long id) {
        List<PolicyAcknowledgementResponse> rows = listAcknowledgements(id);
        StringBuilder sb = new StringBuilder("user_id,email,full_name,acknowledged_at\n");
        for (PolicyAcknowledgementResponse row : rows) {
            sb.append(row.userId()).append(',')
                    .append(csv(row.userEmail())).append(',')
                    .append(csv(row.userFullName())).append(',')
                    .append(row.acknowledgedAt() != null ? row.acknowledgedAt() : "")
                    .append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    @Transactional(readOnly = true)
    public PolicyTaskResponse taskPayload(long policyId, User user) {
        Policy p = require(policyId);
        PolicyVersion v = p.getCurrentVersionId() != null
                ? versionRepository.findById(p.getCurrentVersionId()).orElse(null)
                : null;
        boolean ack = v != null && acknowledgementRepository.existsByPolicyVersionIdAndUserId(v.getId(), user.getId());
        return new PolicyTaskResponse(
                p.getId(),
                p.getTitle(),
                p.getCategory(),
                "v" + p.getMajorVersion() + "." + p.getMinorVersion(),
                v != null ? v.getBodyText() : p.getBodyDraft(),
                v != null ? v.getStorageKey() != null : p.getStorageKeyDraft() != null,
                v != null ? v.getOriginalFilename() : p.getOriginalFilenameDraft(),
                p.getAckDeadline(),
                ack);
    }

    public Policy require(long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found"));
    }

    private void spawnAckTasks(User actor, Policy p, PolicyVersion ver) {
        List<User> targets = resolveAckUsers(p);
        Map<String, String> data = baseData(p);
        data.put("version", "v" + ver.getMajorVersion() + "." + ver.getMinorVersion());
        List<String> recipientIds = new ArrayList<>();
        for (User u : targets) {
            WorkTask task = workTaskRepository
                    .findBySourceModuleAndSourceRecordTypeAndSourceRecordIdAndAssignedToUserId(
                            MyTasksService.MODULE_POLICY, MyTasksService.TYPE_POLICY_ACK, p.getId(), u.getId())
                    .orElseGet(WorkTask::new);
            task.setSourceModule(MyTasksService.MODULE_POLICY);
            task.setSourceRecordType(MyTasksService.TYPE_POLICY_ACK);
            task.setSourceRecordId(p.getId());
            task.setTitle("Acknowledge: " + p.getTitle());
            task.setDetail("v" + ver.getMajorVersion() + "." + ver.getMinorVersion());
            task.setAssignedToUserId(u.getId());
            task.setAssignedByUserId(actor.getId());
            task.setStatus(WorkTaskStatus.PENDING);
            task.setDueDate(p.getAckDeadline());
            task.setSubmittedAt(null);
            workTaskRepository.save(task);
            recipientIds.add(String.valueOf(u.getId()));
        }
        if (!recipientIds.isEmpty()) {
            notificationService.trigger("POLICY_ACK_REQUIRED", String.valueOf(p.getId()),
                    recipientIds, data);
        }
    }

    private List<User> resolveAckUsers(Policy p) {
        return switch (p.getAckScopeType()) {
            case ALL_ACTIVE_USERS -> userRepository.findAll().stream()
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .filter(u -> !u.getRoles().contains(Role.VENDOR))
                    .filter(u -> !u.getRoles().contains(Role.SUPER_ADMIN))
                    .toList();
            case SELECTED_USERS -> ackUserRepository.findByPolicyId(p.getId()).stream()
                    .map(PolicyAckUser::getUserId)
                    .map(id -> userRepository.findById(id).orElse(null))
                    .filter(Objects::nonNull)
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .toList();
            case ORG_UNITS -> {
                Set<Long> unitIds = ackOrgUnitRepository.findByPolicyId(p.getId()).stream()
                        .map(PolicyAckOrgUnit::getOrgUnitId)
                        .collect(Collectors.toSet());
                Set<Long> userIds = new HashSet<>();
                for (Long unitId : unitIds) {
                    userOrgUnitRepository.findByOrgUnitId(unitId).stream()
                            .map(UserOrgUnit::getUserId)
                            .forEach(userIds::add);
                }
                yield userIds.stream()
                        .map(id -> userRepository.findById(id).orElse(null))
                        .filter(Objects::nonNull)
                        .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                        .toList();
            }
        };
    }

    private boolean isInAckScope(Policy p, User user) {
        return resolveAckUsers(p).stream().anyMatch(u -> u.getId().equals(user.getId()));
    }

    private void replaceFrameworks(long policyId, List<Long> frameworkIds) {
        frameworkRepository.deleteByPolicyId(policyId);
        if (frameworkIds == null) {
            return;
        }
        for (Long fwId : new HashSet<>(frameworkIds)) {
            Framework fw = frameworkQueryService.findFramework(fwId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Framework not found: " + fwId));
            PolicyFramework link = new PolicyFramework();
            link.setPolicyId(policyId);
            link.setFrameworkId(fw.getId());
            link.setFrameworkCode(fw.getCode());
            link.setFrameworkName(fw.getName());
            frameworkRepository.save(link);
        }
    }

    private void replaceAckScope(Policy p, List<Long> orgUnitIds, List<Long> userIds) {
        ackOrgUnitRepository.deleteByPolicyId(p.getId());
        ackUserRepository.deleteByPolicyId(p.getId());
        if (p.getAckScopeType() == PolicyAckScopeType.ORG_UNITS) {
            if (orgUnitIds == null || orgUnitIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select organisation units for ack scope");
            }
            for (Long unitId : new HashSet<>(orgUnitIds)) {
                PolicyAckOrgUnit link = new PolicyAckOrgUnit();
                link.setPolicyId(p.getId());
                link.setOrgUnitId(unitId);
                ackOrgUnitRepository.save(link);
            }
        } else if (p.getAckScopeType() == PolicyAckScopeType.SELECTED_USERS) {
            if (userIds == null || userIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select users for ack scope");
            }
            for (Long userId : new HashSet<>(userIds)) {
                if (!userRepository.existsById(userId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found: " + userId);
                }
                PolicyAckUser link = new PolicyAckUser();
                link.setPolicyId(p.getId());
                link.setUserId(userId);
                ackUserRepository.save(link);
            }
        }
    }

    private void assertCategoryAllowed(PolicyCategory category) {
        if (category == PolicyCategory.GENERAL_HR) {
            return;
        }
        String tenantId = TenantContext.getTenantId();
        ModuleCode code = switch (category) {
            case ISMS -> ModuleCode.ISMS;
            case BCMS -> ModuleCode.BCMS;
            case ITSM -> ModuleCode.ITSM;
            case PRIVACY -> ModuleCode.PRIVACY;
            case ERM -> ModuleCode.ERM;
            case GENERAL_HR -> null;
        };
        if (code == null || tenantId == null || !policyCategoryVisible(tenantId, code, true)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Category " + category + " requires a licensed module");
        }
    }

    private boolean policyCategoryVisible(String tenantId, ModuleCode code, boolean write) {
        return moduleLicensingService.isEnabled(tenantId, code)
                && adminModuleAccessService.mayUse(code, write);
    }

    private PolicySummaryResponse toSummary(Policy p) {
        int expected = 0;
        int count = 0;
        if (p.isAckRequired() && p.getCurrentVersionId() != null
                && (p.getStatus() == PolicyStatus.APPROVED || p.getStatus() == PolicyStatus.DUE_FOR_REVIEW)) {
            expected = resolveAckUsers(p).size();
            count = (int) acknowledgementRepository.countByPolicyVersionId(p.getCurrentVersionId());
        }
        return PolicySummaryResponse.from(p, count, expected);
    }

    private PolicyDetailResponse toDetail(User viewer, Policy p) {
        PolicyVersion current = p.getCurrentVersionId() != null
                ? versionRepository.findById(p.getCurrentVersionId()).orElse(null)
                : null;
        boolean ack = current != null
                && acknowledgementRepository.existsByPolicyVersionIdAndUserId(current.getId(), viewer.getId());
        return new PolicyDetailResponse(
                toSummary(p),
                p.getBodyDraft(),
                p.getStorageKeyDraft() != null,
                p.getOriginalFilenameDraft(),
                p.getRejectReason(),
                p.getReviewNotes(),
                frameworkRepository.findByPolicyId(p.getId()).stream()
                        .map(f -> new PolicyFrameworkResponse(f.getFrameworkId(), f.getFrameworkCode(), f.getFrameworkName()))
                        .toList(),
                ackOrgUnitRepository.findByPolicyId(p.getId()).stream().map(PolicyAckOrgUnit::getOrgUnitId).toList(),
                ackUserRepository.findByPolicyId(p.getId()).stream().map(PolicyAckUser::getUserId).toList(),
                current != null ? toVersion(current) : null,
                ack);
    }

    private PolicyVersionResponse toVersion(PolicyVersion v) {
        return new PolicyVersionResponse(
                v.getId(),
                v.getPolicyId(),
                v.getMajorVersion(),
                v.getMinorVersion(),
                "v" + v.getMajorVersion() + "." + v.getMinorVersion(),
                v.getBodyText(),
                v.getStorageKey() != null,
                v.getOriginalFilename(),
                v.getContentType(),
                v.getSizeBytes(),
                v.getApprovedAt(),
                v.getApprovedByUserId(),
                v.getCreatedAt());
    }

    private Map<String, String> baseData(Policy p) {
        Map<String, String> data = new HashMap<>();
        data.put("title", p.getTitle());
        data.put("reviewDate", p.getReviewDate() != null ? p.getReviewDate().toString() : "-");
        data.put("ackDeadline", p.getAckDeadline() != null ? p.getAckDeadline().toString() : "-");
        data.put("version", "v" + p.getMajorVersion() + "." + p.getMinorVersion());
        return data;
    }

    private void assertAdmin(User user) {
        if (user.getRoles().stream().noneMatch(r -> r == Role.OWNER || r == Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private void audit(User user, AuditActionType action, Policy p) {
        auditTrailService.record(action, "POLICY", user.getId(),
                user.getRoles().stream().map(Enum::name).sorted().reduce((a, b) -> a + "," + b).orElse(""),
                "POLICY", String.valueOf(p.getId()), p.getTitle(), null, null, null, null, null);
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
