package com.a9.arbor.grc.modules.risk.service;

import com.a9.arbor.grc.modules.evidence.domain.EvidenceLink;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceLinkRepository;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceRepository;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.risk.domain.RiskFormDraft;
import com.a9.arbor.grc.modules.risk.domain.RiskProject;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.repo.RiskFormDraftRepository;
import com.a9.arbor.grc.modules.risk.repo.RiskProjectRepository;
import com.a9.arbor.grc.modules.risk.web.dto.CreateRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.RiskFormDraftRequest;
import com.a9.arbor.grc.modules.risk.web.dto.RiskFormDraftResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskRecordResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Unfinished IS / ERM create forms ("Save draft"). Owner-only. Reference attachments are linked
 * as {@code RISK / RISK_DRAFT / draftId} and move to the risk when the draft is created.
 */
@Service
public class RiskFormDraftService {

    public static final String DRAFT_RECORD_TYPE = "RISK_DRAFT";
    private static final String RISK_MODULE = "RISK";
    private static final String RISK_RECORD_TYPE = "RISK_RECORD";
    private static final int MAX_PAYLOAD_CHARS = 200_000;
    private static final int MAX_TITLE = 500;

    private final RiskFormDraftRepository draftRepository;
    private final RiskProjectRepository riskProjectRepository;
    private final RiskService riskService;
    private final EvidenceLinkRepository evidenceLinkRepository;
    private final EvidenceRepository evidenceRepository;
    private final ObjectMapper objectMapper;

    public RiskFormDraftService(RiskFormDraftRepository draftRepository,
                                RiskProjectRepository riskProjectRepository,
                                RiskService riskService,
                                EvidenceLinkRepository evidenceLinkRepository,
                                EvidenceRepository evidenceRepository,
                                ObjectMapper objectMapper) {
        this.draftRepository = draftRepository;
        this.riskProjectRepository = riskProjectRepository;
        this.riskService = riskService;
        this.evidenceLinkRepository = evidenceLinkRepository;
        this.evidenceRepository = evidenceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<RiskFormDraftResponse> list(User admin, RiskType type, Long projectId) {
        if (type == null || projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "riskType and projectId are required");
        }
        riskService.assertLicensed(type);
        return draftRepository
                .findByCreatedByUserIdAndRiskTypeAndRiskProjectIdOrderByUpdatedAtDesc(admin.getId(), type, projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RiskFormDraftResponse get(User admin, long id) {
        RiskFormDraft d = requireOwn(admin, id);
        riskService.assertLicensed(d.getRiskType());
        return toResponse(d);
    }

    @Transactional
    public RiskFormDraftResponse create(User admin, RiskFormDraftRequest req) {
        if (req == null || req.riskType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk type is required");
        }
        riskService.assertLicensed(req.riskType());
        requireProject(req.riskType(), req.riskProjectId());
        RiskFormDraft d = new RiskFormDraft();
        d.setRiskType(req.riskType());
        d.setRiskProjectId(req.riskProjectId());
        d.setCreatedByUserId(admin.getId());
        applyPayload(d, req.payload());
        return toResponse(draftRepository.save(d));
    }

    @Transactional
    public RiskFormDraftResponse update(User admin, long id, RiskFormDraftRequest req) {
        RiskFormDraft d = requireOwn(admin, id);
        riskService.assertLicensed(d.getRiskType());
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft content is required");
        }
        applyPayload(d, req.payload());
        d.setUpdatedAt(Instant.now());
        return toResponse(draftRepository.save(d));
    }

    @Transactional
    public void delete(User admin, long id) {
        RiskFormDraft d = requireOwn(admin, id);
        riskService.assertLicensed(d.getRiskType());
        for (EvidenceLink link : draftLinks(d.getId())) {
            Long evidenceId = link.getEvidenceId();
            evidenceLinkRepository.delete(link);
            evidenceLinkRepository.flush();
            if (evidenceId != null && !evidenceLinkRepository.existsByEvidenceId(evidenceId)) {
                evidenceRepository.findById(evidenceId).ifPresent(e -> {
                    e.setDeletedAt(Instant.now());
                    evidenceRepository.save(e);
                });
            }
        }
        draftRepository.delete(d);
    }

    /**
     * Creates the real risk with the normal required-field checks, moves the draft's reference
     * attachments onto it and removes the draft. On any validation error nothing changes.
     */
    @Transactional
    public RiskRecordResponse createRisk(User admin, long id, CreateRiskRequest req) {
        RiskFormDraft d = requireOwn(admin, id);
        if (req == null || req.riskType() != d.getRiskType()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk type must match the draft");
        }
        if (!Objects.equals(req.riskProjectId(), d.getRiskProjectId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk project must match the draft");
        }
        RiskRecordResponse created = riskService.create(admin, req);
        for (EvidenceLink link : draftLinks(d.getId())) {
            link.setRecordType(RISK_RECORD_TYPE);
            link.setRecordId(String.valueOf(created.id()));
            evidenceLinkRepository.save(link);
        }
        draftRepository.delete(d);
        return riskService.get(admin, created.id());
    }

    private void applyPayload(RiskFormDraft d, JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft content must be a JSON object");
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft content is not valid");
        }
        if (json.length() > MAX_PAYLOAD_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft is too large to save");
        }
        d.setPayloadJson(json);
        JsonNode t = payload.get("title");
        String title = t != null && t.isTextual() ? t.asText().trim() : "";
        d.setTitle(title.isEmpty() ? null : title.substring(0, Math.min(MAX_TITLE, title.length())));
    }

    private RiskFormDraft requireOwn(User admin, long id) {
        return draftRepository.findByIdAndCreatedByUserId(id, admin.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft not found"));
    }

    private void requireProject(RiskType type, Long projectId) {
        if (projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Open a risk project before saving a draft");
        }
        RiskProject p = riskProjectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk project not found"));
        if (p.getProjectType() != type) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk type must match the project");
        }
    }

    private List<EvidenceLink> draftLinks(Long draftId) {
        return evidenceLinkRepository.findByModuleCodeAndRecordTypeAndRecordIdOrderByLinkedAtDesc(
                RISK_MODULE, DRAFT_RECORD_TYPE, String.valueOf(draftId));
    }

    private RiskFormDraftResponse toResponse(RiskFormDraft d) {
        JsonNode payload;
        try {
            payload = objectMapper.readTree(d.getPayloadJson());
        } catch (JsonProcessingException e) {
            payload = objectMapper.createObjectNode();
        }
        return new RiskFormDraftResponse(d.getId(), d.getRiskType(), d.getRiskProjectId(), d.getTitle(), payload,
                draftLinks(d.getId()).size(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
