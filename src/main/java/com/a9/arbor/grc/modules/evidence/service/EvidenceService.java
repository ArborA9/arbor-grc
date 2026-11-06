package com.a9.arbor.grc.modules.evidence.service;

import com.a9.arbor.grc.config.ArborEvidenceProperties;
import com.a9.arbor.grc.modules.evidence.domain.Evidence;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceClassification;
import com.a9.arbor.grc.modules.evidence.domain.RiskEvidenceStage;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceLink;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceStatus;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceVersion;
import com.a9.arbor.grc.modules.evidence.domain.VirusScanStatus;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceLinkRepository;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceRepository;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceVersionRepository;
import com.a9.arbor.grc.modules.evidence.web.dto.CreateEvidenceLinkRequest;
import com.a9.arbor.grc.modules.evidence.web.dto.EvidenceDetailResponse;
import com.a9.arbor.grc.modules.evidence.web.dto.EvidenceLinkResponse;
import com.a9.arbor.grc.modules.evidence.web.dto.EvidenceSummaryResponse;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.ContributorAccessService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final EvidenceVersionRepository versionRepository;
    private final EvidenceLinkRepository linkRepository;
    private final EvidenceFileStore fileStore;
    private final EvidenceVirusScanPolicy virusScanPolicy;
    private final EvidenceDeleteGuard deleteGuard;
    private final ContributorAccessService contributorAccess;
    private final boolean blockDownloadUnlessClean;

    public EvidenceService(EvidenceRepository evidenceRepository,
                           EvidenceVersionRepository versionRepository,
                           EvidenceLinkRepository linkRepository,
                           EvidenceFileStore fileStore,
                           EvidenceVirusScanPolicy virusScanPolicy,
                           EvidenceDeleteGuard deleteGuard,
                           ContributorAccessService contributorAccess,
                           ArborEvidenceProperties evidenceProperties) {
        this.evidenceRepository = evidenceRepository;
        this.versionRepository = versionRepository;
        this.linkRepository = linkRepository;
        this.fileStore = fileStore;
        this.virusScanPolicy = virusScanPolicy;
        this.deleteGuard = deleteGuard;
        this.contributorAccess = contributorAccess;
        this.blockDownloadUnlessClean = evidenceProperties.isBlockDownloadUnlessClean();
    }

    @Transactional(readOnly = true)
    public Page<EvidenceSummaryResponse> list(User user,
                                              String q,
                                              EvidenceClassification classification,
                                              EvidenceStatus status,
                                              Pageable pageable) {
        Set<EvidenceClassification> allowed = allowedClassifications(user);
        if (classification != null && !allowed.contains(classification)) {
            return Page.empty(pageable);
        }
        String query = (q == null || q.isBlank()) ? null : q.trim();
        Page<Evidence> page;
        if (RoleAccess.isContributorOnly(user)) {
            List<String> gapIds = contributorAccess.assignedGapItemIds(user);
            List<String> riskIds = contributorAccess.visibleRiskRecordIds(user);
            List<String> capaIds = contributorAccess.ownedCapaIds(user);
            page = evidenceRepository.searchForContributor(
                    query, classification, status, allowed, user.getId(),
                    emptyToSentinel(gapIds), emptyToSentinel(riskIds), emptyToSentinel(capaIds), pageable);
        } else {
            page = evidenceRepository.search(query, classification, status, allowed, pageable);
        }
        page.getContent().forEach(this::refreshExpired);
        return page.map(e -> {
            EvidenceVersion latest = versionRepository
                    .findByEvidenceIdAndVersionNumber(e.getId(), e.getCurrentVersion())
                    .orElse(null);
            int links = linkRepository.findByEvidenceIdOrderByLinkedAtDesc(e.getId()).size();
            return EvidenceSummaryResponse.from(e, latest, links);
        });
    }

    /**
     * Evidence linked to a module record (e.g. GAP / GAP_ITEM / {id}).
     * Filtered by the caller's classification permissions.
     */
    @Transactional(readOnly = true)
    public List<EvidenceSummaryResponse> listForRecord(User user,
                                                       String moduleCode,
                                                       String recordType,
                                                       String recordId) {
        if (moduleCode == null || moduleCode.isBlank()
                || recordType == null || recordType.isBlank()
                || recordId == null || recordId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "moduleCode, recordType and recordId are required");
        }
        contributorAccess.assertCanAccessLinkedRecord(user, moduleCode, recordType, recordId);
        Set<EvidenceClassification> allowed = allowedClassifications(user);
        boolean contributorOnAssignedRecord = RoleAccess.isContributorOnly(user);
        List<EvidenceLink> links = linkRepository.findByModuleCodeAndRecordTypeAndRecordIdOrderByLinkedAtDesc(
                moduleCode.trim().toUpperCase(Locale.ROOT),
                recordType.trim(),
                recordId.trim());
        if (links.isEmpty()) {
            return List.of();
        }
        Map<Long, EvidenceLink> firstLink = new LinkedHashMap<>();
        for (EvidenceLink link : links) {
            firstLink.putIfAbsent(link.getEvidenceId(), link);
        }
        Map<Long, Evidence> byId = new LinkedHashMap<>();
        for (Evidence e : evidenceRepository.findByIdInAndDeletedAtIsNull(firstLink.keySet())) {
            byId.put(e.getId(), e);
        }
        List<EvidenceSummaryResponse> out = new ArrayList<>();
        for (Long evidenceId : firstLink.keySet()) {
            Evidence e = byId.get(evidenceId);
            if (e == null) {
                continue;
            }
            // Assigned-record context: contributors see all classifications (reference attachments, etc.).
            // Vault browse ({@link #list}) still restricts Secret/Sensitive.
            if (!contributorOnAssignedRecord && !allowed.contains(e.getClassification())) {
                continue;
            }
            refreshExpired(e);
            EvidenceVersion latest = versionRepository
                    .findByEvidenceIdAndVersionNumber(e.getId(), e.getCurrentVersion())
                    .orElse(null);
            int linkCount = linkRepository.findByEvidenceIdOrderByLinkedAtDesc(e.getId()).size();
            out.add(EvidenceSummaryResponse.from(
                    e, latest, linkCount, firstLink.get(evidenceId).getWorkflowStage()));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public EvidenceDetailResponse get(User user, long id) {
        Evidence e = requireVisible(user, id);
        refreshExpired(e);
        return EvidenceDetailResponse.from(
                e,
                versionRepository.findByEvidenceIdOrderByVersionNumberDesc(id),
                linkRepository.findByEvidenceIdOrderByLinkedAtDesc(id));
    }

    /**
     * Persist a text snapshot as evidence (e.g. Risk Acceptance Form → Confidential).
     * Filename must use an allowed extension (typically {@code .pdf}).
     */
    @Transactional
    public EvidenceDetailResponse createLinkedTextDocument(User user,
                                                           String title,
                                                           EvidenceClassification classification,
                                                           String filename,
                                                           String contentType,
                                                           byte[] bytes,
                                                           String moduleCode,
                                                           String recordType,
                                                           String recordId,
                                                           String workflowStage) {
        if (!taggableClassifications(user).contains(classification)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Classification not permitted for your role");
        }
        assertRiskEvidenceLimits(user, bytes == null ? 0 : bytes.length, moduleCode, recordType, recordId);
        Evidence e = new Evidence();
        e.setTitle(title == null || title.isBlank() ? filename : title.trim());
        e.setClassification(classification);
        e.setExpiresOn(null);
        e.setStatus(EvidenceStatus.ACTIVE);
        e.setCurrentVersion(1);
        e.setCreatedByUserId(user.getId());
        e = evidenceRepository.save(e);

        EvidenceFileStore.StoredFile stored = fileStore.storeBytes(
                e.getId(), 1, filename, contentType, bytes);
        EvidenceVersion version = new EvidenceVersion();
        version.setEvidenceId(e.getId());
        version.setVersionNumber(1);
        version.setOriginalFilename(stored.originalFilename());
        version.setContentType(stored.contentType());
        version.setSizeBytes(stored.sizeBytes());
        version.setStorageKey(stored.storageKey());
        version.setVirusScanStatus(virusScanPolicy.statusForNewUpload());
        version.setUploadedByUserId(user.getId());
        versionRepository.save(version);

        if (moduleCode != null && !moduleCode.isBlank()
                && recordType != null && !recordType.isBlank()
                && recordId != null && !recordId.isBlank()) {
            addLinkInternal(e.getId(), user.getId(), moduleCode, recordType, recordId, workflowStage);
        }
        return get(user, e.getId());
    }

    @Transactional(readOnly = true)
    public int countLinksForRecord(String moduleCode, String recordType, String recordId) {
        return linkRepository.findByModuleCodeAndRecordTypeAndRecordIdOrderByLinkedAtDesc(
                moduleCode.trim().toUpperCase(Locale.ROOT), recordType.trim(), recordId.trim()).size();
    }

    @Transactional(readOnly = true)
    public boolean hasNonIdentificationLinkOnOrAfter(String moduleCode, String recordType, String recordId,
                                                     Instant onOrAfter) {
        if (onOrAfter == null) {
            return false;
        }
        return linkRepository
                .findByModuleCodeAndRecordTypeAndRecordIdOrderByLinkedAtDesc(
                        moduleCode.trim().toUpperCase(Locale.ROOT), recordType.trim(), recordId.trim())
                .stream()
                .filter(l -> {
                    String stage = l.getWorkflowStage();
                    return stage == null || !"IDENTIFICATION".equalsIgnoreCase(stage.trim());
                })
                .anyMatch(l -> l.getLinkedAt() != null && !l.getLinkedAt().isBefore(onOrAfter));
    }

    @Transactional
    public EvidenceDetailResponse upload(User user,
                                         String title,
                                         String classificationRaw,
                                         LocalDate expiresOn,
                                         MultipartFile file,
                                         String moduleCode,
                                         String recordType,
                                         String recordId,
                                         String workflowStage) {
        EvidenceClassification classification = parseClassification(classificationRaw);
        if (!taggableClassifications(user).contains(classification)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Classification not permitted for your role");
        }
        if (RoleAccess.isContributorOnly(user)) {
            if (moduleCode == null || moduleCode.isBlank()
                    || recordType == null || recordType.isBlank()
                    || recordId == null || recordId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Contributors must link evidence to an assigned Gap, Risk, CAPA, or Privacy record");
            }
            contributorAccess.assertCanAccessLinkedRecord(user, moduleCode, recordType, recordId);
        } else if (user.getRoles().contains(Role.VENDOR)) {
            if (moduleCode == null || !"TPRM".equalsIgnoreCase(moduleCode)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Vendors may only upload evidence for TPRM questionnaires");
            }
        }
        assertRiskEvidenceLimits(user, file == null ? 0 : file.getSize(), moduleCode, recordType, recordId);
        Evidence e = new Evidence();
        e.setTitle(title == null || title.isBlank() ? defaultTitle(file) : title.trim());
        e.setClassification(classification);
        e.setExpiresOn(expiresOn);
        e.setStatus(resolveStatus(expiresOn));
        e.setCurrentVersion(1);
        e.setCreatedByUserId(user.getId());
        e = evidenceRepository.save(e);

        EvidenceFileStore.StoredFile stored = fileStore.store(e.getId(), 1, file);
        EvidenceVersion version = new EvidenceVersion();
        version.setEvidenceId(e.getId());
        version.setVersionNumber(1);
        version.setOriginalFilename(stored.originalFilename());
        version.setContentType(stored.contentType());
        version.setSizeBytes(stored.sizeBytes());
        version.setStorageKey(stored.storageKey());
        version.setVirusScanStatus(virusScanPolicy.statusForNewUpload());
        version.setUploadedByUserId(user.getId());
        versionRepository.save(version);

        if (moduleCode != null && !moduleCode.isBlank()
                && recordType != null && !recordType.isBlank()
                && recordId != null && !recordId.isBlank()) {
            addLinkInternal(e.getId(), user.getId(), moduleCode, recordType, recordId, workflowStage);
        }

        return get(user, e.getId());
    }

    @Transactional
    public EvidenceDetailResponse addVersion(User user, long id, MultipartFile file) {
        Evidence e = requireVisible(user, id);
        if (RoleAccess.isContributorOnly(user) && !user.getId().equals(e.getCreatedByUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Contributors can only add versions to evidence they uploaded");
        }
        assertRiskEvidenceSizeIfLinked(e.getId(), file == null ? 0 : file.getSize());
        int next = e.getCurrentVersion() + 1;
        EvidenceFileStore.StoredFile stored = fileStore.store(e.getId(), next, file);
        EvidenceVersion version = new EvidenceVersion();
        version.setEvidenceId(e.getId());
        version.setVersionNumber(next);
        version.setOriginalFilename(stored.originalFilename());
        version.setContentType(stored.contentType());
        version.setSizeBytes(stored.sizeBytes());
        version.setStorageKey(stored.storageKey());
        version.setVirusScanStatus(virusScanPolicy.statusForNewUpload());
        version.setUploadedByUserId(user.getId());
        versionRepository.save(version);
        e.setCurrentVersion(next);
        e.setUpdatedAt(Instant.now());
        evidenceRepository.save(e);
        return get(user, id);
    }

    @Transactional
    public EvidenceLinkResponse addLink(User user, long id, CreateEvidenceLinkRequest req) {
        Evidence e = requireVisible(user, id);
        contributorAccess.assertCanAccessLinkedRecord(user, req.moduleCode(), req.recordType(), req.recordId());
        EvidenceVersion latest = versionRepository
                .findByEvidenceIdAndVersionNumber(e.getId(), e.getCurrentVersion())
                .orElse(null);
        long size = latest != null ? latest.getSizeBytes() : 0;
        assertRiskEvidenceLimits(user, size, req.moduleCode(), req.recordType(), req.recordId());
        EvidenceLink link = addLinkInternal(
                e.getId(),
                user.getId(),
                req.moduleCode(),
                req.recordType(),
                req.recordId(),
                req.workflowStage());
        e.setUpdatedAt(Instant.now());
        evidenceRepository.save(e);
        return EvidenceLinkResponse.from(link);
    }

    @Transactional
    public void removeLink(User user, long evidenceId, long linkId) {
        requireVisible(user, evidenceId);
        EvidenceLink link = linkRepository.findByIdAndEvidenceId(linkId, evidenceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));
        if (RoleAccess.isContributorOnly(user)) {
            contributorAccess.assertCanAccessLinkedRecord(
                    user, link.getModuleCode(), link.getRecordType(), link.getRecordId());
            deleteGuard.assertContributorUnlinkAllowed(link);
        }
        unlinkAndMaybeSoftDelete(user, evidenceId, link);
    }

    @Transactional
    public void unlinkFromRecord(User user, long evidenceId, String moduleCode, String recordType, String recordId) {
        requireVisible(user, evidenceId);
        contributorAccess.assertCanAccessLinkedRecord(user, moduleCode, recordType, recordId);
        String module = moduleCode.trim().toUpperCase(Locale.ROOT);
        String type = recordType.trim();
        String rec = recordId.trim();
        EvidenceLink link = linkRepository
                .findByModuleCodeAndRecordTypeAndRecordIdOrderByLinkedAtDesc(module, type, rec)
                .stream()
                .filter(l -> l.getEvidenceId() != null && l.getEvidenceId() == evidenceId)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));
        if (RoleAccess.isContributorOnly(user)) {
            deleteGuard.assertContributorUnlinkAllowed(link);
        }
        unlinkAndMaybeSoftDelete(user, evidenceId, link);
    }

    private void unlinkAndMaybeSoftDelete(User user, long evidenceId, EvidenceLink link) {
        linkRepository.delete(link);
        Evidence e = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found"));
        if (RoleAccess.isContributorOnly(user)
                && user.getId().equals(e.getCreatedByUserId())
                && !linkRepository.existsByEvidenceId(evidenceId)) {
            deleteGuard.assertSoftDeleteAllowed(evidenceId);
            e.setDeletedAt(Instant.now());
            evidenceRepository.save(e);
        }
    }

    @Transactional
    public void softDelete(User user, long id) {
        Evidence e = requireVisible(user, id);
        deleteGuard.assertSoftDeleteAllowed(id);
        e.setDeletedAt(Instant.now());
        evidenceRepository.save(e);
    }

    @Transactional(readOnly = true)
    public Download download(User user, long id, Integer versionNumber) {
        Evidence e = requireVisible(user, id);
        int ver = versionNumber == null ? e.getCurrentVersion() : versionNumber;
        EvidenceVersion version = versionRepository.findByEvidenceIdAndVersionNumber(id, ver)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
        if (blockDownloadUnlessClean && version.getVirusScanStatus() != VirusScanStatus.CLEAN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Download blocked until virus scan completes with a clean result");
        }
        Resource resource = fileStore.open(version.getStorageKey());
        return new Download(resource, version.getOriginalFilename(), version.getContentType());
    }

    private void assertRiskEvidenceLimits(User user,
                                          long sizeBytes,
                                          String moduleCode,
                                          String recordType,
                                          String recordId) {
        if (!RiskEvidenceLimits.appliesTo(moduleCode)) {
            return;
        }
        if (sizeBytes > RiskEvidenceLimits.MAX_FILE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds max size of 10MB");
        }
        if (recordType == null || recordType.isBlank() || recordId == null || recordId.isBlank()) {
            return;
        }
        long existing = linkRepository.countActiveByUploaderAndRecord(
                user.getId(),
                moduleCode.trim().toUpperCase(Locale.ROOT),
                recordType.trim(),
                recordId.trim());
        if (existing >= RiskEvidenceLimits.MAX_FILES_PER_USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You can attach at most " + RiskEvidenceLimits.MAX_FILES_PER_USER + " files to "
                            + RiskEvidenceLimits.recordNoun(moduleCode, recordType));
        }
    }

    private void assertRiskEvidenceSizeIfLinked(long evidenceId, long sizeBytes) {
        boolean linkedToRisk = linkRepository.findByEvidenceIdOrderByLinkedAtDesc(evidenceId).stream()
                .anyMatch(l -> RiskEvidenceLimits.appliesTo(l.getModuleCode()));
        if (linkedToRisk && sizeBytes > RiskEvidenceLimits.MAX_FILE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds max size of 10MB");
        }
    }

    private EvidenceLink addLinkInternal(long evidenceId,
                                         long userId,
                                         String moduleCode,
                                         String recordType,
                                         String recordId,
                                         String workflowStage) {
        String module = moduleCode.trim().toUpperCase(Locale.ROOT);
        String stage = workflowStage == null || workflowStage.isBlank() ? null : workflowStage.trim();
        if (RiskEvidenceStage.isRiskModule(module)) {
            stage = RiskEvidenceStage.requireForRiskLink(stage);
        }
        EvidenceLink link = new EvidenceLink();
        link.setEvidenceId(evidenceId);
        link.setModuleCode(module);
        link.setRecordType(recordType.trim());
        link.setRecordId(recordId.trim());
        link.setWorkflowStage(stage);
        link.setLinkedByUserId(userId);
        try {
            return linkRepository.save(link);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Link already exists for this record", ex);
        }
    }

    private Evidence requireVisible(User user, long id) {
        Evidence e = evidenceRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found"));
        if (RoleAccess.isContributorOnly(user)) {
            // Own uploads or evidence linked to an assigned record (any classification).
            // Unlinked vault items stay hidden unless the contributor created them.
            if (!contributorMaySee(user, e)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Evidence not linked to your assignments");
            }
        } else if (!allowedClassifications(user).contains(e.getClassification())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Classification not permitted for your role");
        }
        refreshExpired(e);
        return e;
    }

    private boolean contributorMaySee(User user, Evidence e) {
        if (user.getId().equals(e.getCreatedByUserId())) {
            return true;
        }
        for (EvidenceLink link : linkRepository.findByEvidenceIdOrderByLinkedAtDesc(e.getId())) {
            if (contributorAccess.canAccessLinkedRecord(
                    user, link.getModuleCode(), link.getRecordType(), link.getRecordId())) {
                return true;
            }
        }
        return false;
    }

    private static List<String> emptyToSentinel(List<String> ids) {
        return ids == null || ids.isEmpty() ? List.of("__none__") : ids;
    }

    private void refreshExpired(Evidence e) {
        if (e.getExpiresOn() != null
                && e.getExpiresOn().isBefore(LocalDate.now())
                && e.getStatus() != EvidenceStatus.EXPIRED) {
            e.setStatus(EvidenceStatus.EXPIRED);
            evidenceRepository.save(e);
        }
    }

    private static EvidenceStatus resolveStatus(LocalDate expiresOn) {
        if (expiresOn != null && expiresOn.isBefore(LocalDate.now())) {
            return EvidenceStatus.EXPIRED;
        }
        return EvidenceStatus.ACTIVE;
    }

    private static EvidenceClassification parseClassification(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Classification is required");
        }
        try {
            return EvidenceClassification.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid classification. Use SECRET, SENSITIVE, CONFIDENTIAL, or OPEN", ex);
        }
    }

    private static String defaultTitle(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name == null || name.isBlank() ? "Untitled evidence" : name;
    }

    /**
     * Vault browse classifications. Contributors still cannot list others' Secret/Sensitive in the
     * vault; linked assigned records use {@link #listForRecord} / {@link #requireVisible} instead.
     */
    static Set<EvidenceClassification> allowedClassifications(User user) {
        if (user.getRoles().contains(Role.VENDOR)) {
            return EnumSet.of(EvidenceClassification.OPEN);
        }
        if (RoleAccess.isContributorOnly(user)) {
            return EnumSet.of(EvidenceClassification.CONFIDENTIAL, EvidenceClassification.OPEN);
        }
        return EnumSet.allOf(EvidenceClassification.class);
    }

    /** Tag at upload — all four levels (Secret / Sensitive / Confidential / Open). */
    static Set<EvidenceClassification> taggableClassifications(User user) {
        if (user.getRoles().contains(Role.VENDOR)) {
            return EnumSet.of(EvidenceClassification.OPEN);
        }
        return EnumSet.allOf(EvidenceClassification.class);
    }

    @Transactional(readOnly = true)
    public String filenameForEvidence(Long evidenceId) {
        if (evidenceId == null) {
            return null;
        }
        return evidenceRepository.findById(evidenceId)
                .flatMap(e -> versionRepository.findByEvidenceIdAndVersionNumber(e.getId(), e.getCurrentVersion()))
                .map(EvidenceVersion::getOriginalFilename)
                .orElse(null);
    }

    public record Download(Resource resource, String filename, String contentType) {
    }
}
