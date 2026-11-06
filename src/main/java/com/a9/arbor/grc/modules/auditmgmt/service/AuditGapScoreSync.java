package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistAssessment;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistItem;
import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Maps Gap assessment control scores into audit checklist assessments (UAE / NCEMA). */
public final class AuditGapScoreSync {

    private AuditGapScoreSync() {
    }

    public static int applyToChecklist(List<GrcAuditChecklistItem> checklist, List<GapItem> gapItems) {
        Map<Long, GapItem> byNodeId = gapItems.stream()
                .collect(Collectors.toMap(GapItem::getFrameworkNodeId, Function.identity(), (a, b) -> a));
        int updated = 0;
        for (GrcAuditChecklistItem item : checklist) {
            GapItem gapItem = byNodeId.get(item.getFrameworkNodeId());
            if (gapItem == null) {
                continue;
            }
            ChecklistAssessment mapped = mapStatus(gapItem.effectiveImplementationStatus());
            if (mapped != null && mapped != item.getAssessment()) {
                item.setAssessment(mapped);
                updated++;
            }
        }
        return updated;
    }

    static ChecklistAssessment mapStatus(GapImplementationStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case FULLY_IMPLEMENTED -> ChecklistAssessment.YES;
            case PARTIALLY_IMPLEMENTED -> ChecklistAssessment.PARTIAL;
            case PLANNED, NOT_IMPLEMENTED -> ChecklistAssessment.NO;
        };
    }
}
