package com.a9.arbor.grc.modules.gap.service;

import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.repo.GapItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Copies framework What it means / Evidence onto gap controls that have never stored a value.
 * A blank string is an admin clear and is left alone. Runs in its own transaction so a
 * read-only assessment load can still persist the default.
 */
@Service
public class GapFrameworkMeaning {

    private final GapItemRepository itemRepository;

    public GapFrameworkMeaning(GapItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fillEmpty(long assessmentId, Map<Long, FrameworkNodeResponse> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (GapItem item : itemRepository.findByGapAssessmentIdOrderByNodeCodeAsc(assessmentId)) {
            FrameworkNodeResponse node = nodes.get(item.getFrameworkNodeId());
            if (node == null) {
                continue;
            }
            if (item.getWhatItMeans() == null) {
                String value = catalogAttr(node, "what_it_means");
                if (value != null) {
                    item.setWhatItMeans(value);
                    changed = true;
                }
            }
            if (item.getEvidenceGuidance() == null) {
                String value = catalogAttr(node, "evidence", "evidence_examples");
                if (value != null) {
                    item.setEvidenceGuidance(value);
                    changed = true;
                }
            }
        }
        if (changed) {
            itemRepository.flush();
        }
    }

    static String catalogAttr(FrameworkNodeResponse node, String... keys) {
        if (node == null || node.attributes() == null) {
            return null;
        }
        for (String key : keys) {
            String value = node.attributes().get(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
