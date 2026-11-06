package com.a9.arbor.grc.modules.gap.service;

import com.a9.arbor.grc.catalog.domain.CrossFrameworkMapping;
import com.a9.arbor.grc.catalog.domain.Framework;
import com.a9.arbor.grc.catalog.domain.FrameworkNode;
import com.a9.arbor.grc.catalog.repo.FrameworkNodeRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkRepository;
import com.a9.arbor.grc.catalog.service.CrossFrameworkMappingService;
import com.a9.arbor.grc.modules.gap.domain.GapApplicability;
import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapAssessmentStatus;
import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.domain.GapItemReviewStatus;
import com.a9.arbor.grc.modules.gap.repo.GapAssessmentRepository;
import com.a9.arbor.grc.modules.gap.repo.GapItemRepository;
import com.a9.arbor.grc.modules.iam.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * When a Gap control is finalized, apply dual-credit to mapped controls in other framework
 * assessments within the same compliance project (A3 / Journey 3).
 */
@Service
public class CrossFrameworkDualCreditService {

    private final CrossFrameworkMappingService mappingService;
    private final FrameworkNodeRepository nodeRepository;
    private final FrameworkRepository frameworkRepository;
    private final GapAssessmentRepository assessmentRepository;
    private final GapItemRepository itemRepository;

    public CrossFrameworkDualCreditService(CrossFrameworkMappingService mappingService,
                                           FrameworkNodeRepository nodeRepository,
                                           FrameworkRepository frameworkRepository,
                                           GapAssessmentRepository assessmentRepository,
                                           GapItemRepository itemRepository) {
        this.mappingService = mappingService;
        this.nodeRepository = nodeRepository;
        this.frameworkRepository = frameworkRepository;
        this.assessmentRepository = assessmentRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public List<Long> applyOnFinalize(User admin,
                                      GapAssessment sourceAssessment,
                                      GapItem sourceItem,
                                      GapImplementationStatus finalStatus) {
        List<CrossFrameworkMapping> mappings = mappingService.dualCreditMappings(sourceItem.getFrameworkNodeId());
        if (mappings.isEmpty()) {
            return List.of();
        }

        List<Long> refreshedAssessmentIds = new ArrayList<>();
        for (CrossFrameworkMapping mapping : mappings) {
            FrameworkNode targetNode = nodeRepository.findById(mapping.getTargetNodeId()).orElse(null);
            if (targetNode == null) {
                continue;
            }
            Long targetFrameworkId = targetNode.getFrameworkId();
            List<GapAssessment> peerAssessments = assessmentRepository
                    .findByProjectIdAndFrameworkIdAndStatusIn(
                            sourceAssessment.getProjectId(),
                            targetFrameworkId,
                            EnumSet.of(GapAssessmentStatus.DRAFT, GapAssessmentStatus.IN_PROGRESS));
            if (peerAssessments.isEmpty()) {
                continue;
            }

            String creditNote = buildCreditNote(sourceAssessment, sourceItem, mapping);
            for (GapAssessment peer : peerAssessments) {
                Optional<GapItem> targetOpt = itemRepository.findByGapAssessmentIdAndFrameworkNodeId(
                        peer.getId(), targetNode.getId());
                if (targetOpt.isEmpty()) {
                    continue;
                }
                GapItem target = targetOpt.get();
                if (target.getApplicability() == GapApplicability.NOT_APPLICABLE) {
                    continue;
                }
                if (target.getReviewStatus() == GapItemReviewStatus.FINALIZED
                        && target.getFinalImplementationStatus() != null
                        && target.getFinalImplementationStatus() == finalStatus) {
                    continue;
                }

                target.setFinalImplementationStatus(finalStatus);
                target.setReviewStatus(GapItemReviewStatus.FINALIZED);
                target.setAdminReviewedAt(Instant.now());
                target.setAdminReviewedByUserId(admin.getId());
                target.setUpdatedByUserId(admin.getId());
                if (target.getAssignedToUserId() == null && target.getImplementationStatus() == null) {
                    target.setImplementationStatus(finalStatus);
                }
                String existing = target.getAdminReviewNotes();
                target.setAdminReviewNotes(existing == null || existing.isBlank()
                        ? creditNote
                        : existing + "\n" + creditNote);
                target.setScore(GapScoring.scoreOf(finalStatus));
                itemRepository.save(target);
                if (!refreshedAssessmentIds.contains(peer.getId())) {
                    refreshedAssessmentIds.add(peer.getId());
                }
            }
        }
        return refreshedAssessmentIds;
    }

    private String buildCreditNote(GapAssessment sourceAssessment,
                                   GapItem sourceItem,
                                   CrossFrameworkMapping mapping) {
        String sourceFw = sourceAssessment.getFrameworkCode();
        Framework targetFw = frameworkRepository.findById(
                nodeRepository.findById(mapping.getTargetNodeId()).map(FrameworkNode::getFrameworkId).orElse(-1L))
                .orElse(null);
        String targetFwCode = targetFw != null ? targetFw.getCode() : "?";
        return "Dual-credited from " + sourceFw + " " + sourceItem.getNodeCode()
                + " → " + targetFwCode + " (" + mapping.getMappingType().name() + ")";
    }
}
