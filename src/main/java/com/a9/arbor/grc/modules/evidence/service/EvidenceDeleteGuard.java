package com.a9.arbor.grc.modules.evidence.service;

import com.a9.arbor.grc.modules.evidence.domain.EvidenceLink;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceLinkRepository;
import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapAssessmentStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.domain.GapItemReviewStatus;
import com.a9.arbor.grc.modules.gap.repo.GapAssessmentRepository;
import com.a9.arbor.grc.modules.gap.repo.GapItemRepository;
import com.a9.arbor.grc.modules.gap.service.GapEvidenceLinks;
import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;
import com.a9.arbor.grc.modules.risk.repo.RiskRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Blocks soft-delete when evidence is linked to a finalized Gap control or terminal Risk.
 */
@Service
public class EvidenceDeleteGuard {

    private static final Set<RiskStatus> TERMINAL_RISK = EnumSet.of(
            RiskStatus.AVOIDED,
            RiskStatus.TRANSFERRED,
            RiskStatus.CLOSED,
            RiskStatus.AUTO_CLOSED);

    private final EvidenceLinkRepository linkRepository;
    private final GapItemRepository gapItemRepository;
    private final GapAssessmentRepository gapAssessmentRepository;
    private final RiskRecordRepository riskRecordRepository;

    public EvidenceDeleteGuard(EvidenceLinkRepository linkRepository,
                               GapItemRepository gapItemRepository,
                               GapAssessmentRepository gapAssessmentRepository,
                               RiskRecordRepository riskRecordRepository) {
        this.linkRepository = linkRepository;
        this.gapItemRepository = gapItemRepository;
        this.gapAssessmentRepository = gapAssessmentRepository;
        this.riskRecordRepository = riskRecordRepository;
    }

    public void assertSoftDeleteAllowed(long evidenceId) {
        List<EvidenceLink> links = linkRepository.findByEvidenceIdOrderByLinkedAtDesc(evidenceId);
        for (EvidenceLink link : links) {
            String reason = lockReason(link);
            if (reason != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
            }
        }
    }

    /** Contributor may unlink only while the source record is still in their working state. */
    public void assertContributorUnlinkAllowed(EvidenceLink link) {
        String reason = contributorUnlinkLockReason(link);
        if (reason != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
        }
    }

    private String lockReason(EvidenceLink link) {
        String module = link.getModuleCode() == null ? "" : link.getModuleCode().toUpperCase(Locale.ROOT);
        String type = link.getRecordType() == null ? "" : link.getRecordType().trim();
        if ("GAP".equals(module) && ("GAP_ITEM".equals(type) || "GAP_QUESTION".equals(type))) {
            long itemId;
            try {
                if ("GAP_QUESTION".equals(type)) {
                    Long parsed = GapEvidenceLinks.itemIdFromQuestionRecord(link.getRecordId());
                    if (parsed == null) {
                        return null;
                    }
                    itemId = parsed;
                } else {
                    itemId = Long.parseLong(link.getRecordId().trim());
                }
            } catch (NumberFormatException e) {
                return null;
            }
            GapItem item = gapItemRepository.findById(itemId).orElse(null);
            if (item == null) {
                return null;
            }
            if (item.getReviewStatus() == GapItemReviewStatus.FINALIZED) {
                return "Cannot delete evidence linked to finalized Gap control "
                        + item.getNodeCode() + " (item " + item.getId() + ")";
            }
            GapAssessment assessment = gapAssessmentRepository.findById(item.getGapAssessmentId()).orElse(null);
            if (assessment != null && assessment.getStatus() == GapAssessmentStatus.COMPLETED) {
                return "Cannot delete evidence linked to completed Gap assessment "
                        + assessment.getId() + " (control " + item.getNodeCode() + ")";
            }
            return null;
        }
        if ("RISK".equals(module) && "RISK_RECORD".equals(type)) {
            long riskId;
            try {
                riskId = Long.parseLong(link.getRecordId().trim());
            } catch (NumberFormatException e) {
                return null;
            }
            RiskRecord risk = riskRecordRepository.findById(riskId).orElse(null);
            if (risk == null) {
                return null;
            }
            if (TERMINAL_RISK.contains(risk.getStatus())) {
                return "Cannot delete evidence linked to terminal Risk "
                        + risk.getDisplayId() + " (status " + risk.getStatus() + ")";
            }
            return null;
        }
        return null;
    }

    private String contributorUnlinkLockReason(EvidenceLink link) {
        String module = link.getModuleCode() == null ? "" : link.getModuleCode().toUpperCase(Locale.ROOT);
        String type = link.getRecordType() == null ? "" : link.getRecordType().trim();
        if ("GAP".equals(module) && ("GAP_ITEM".equals(type) || "GAP_QUESTION".equals(type))) {
            long itemId;
            try {
                if ("GAP_QUESTION".equals(type)) {
                    Long parsed = GapEvidenceLinks.itemIdFromQuestionRecord(link.getRecordId());
                    if (parsed == null) {
                        return null;
                    }
                    itemId = parsed;
                } else {
                    itemId = Long.parseLong(link.getRecordId().trim());
                }
            } catch (NumberFormatException e) {
                return null;
            }
            GapItem item = gapItemRepository.findById(itemId).orElse(null);
            if (item == null) {
                return null;
            }
            if (item.getReviewStatus() == GapItemReviewStatus.SUBMITTED
                    || item.getReviewStatus() == GapItemReviewStatus.FINALIZED) {
                return "Cannot remove evidence after the control is submitted";
            }
            GapAssessment assessment = gapAssessmentRepository.findById(item.getGapAssessmentId()).orElse(null);
            if (assessment != null && assessment.getStatus() == GapAssessmentStatus.COMPLETED) {
                return "Cannot remove evidence from a completed Gap assessment";
            }
            return null;
        }
        return lockReason(link);
    }
}
