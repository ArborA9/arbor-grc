package com.a9.arbor.grc.modules.gap.service;

import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;
import com.a9.arbor.grc.modules.gap.domain.GapApplicability;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.web.dto.GapLayerScoreSummary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** NCEMA V3 pillar scoring: six pillars from level-1 framework nodes (req §15.6). */
public final class NcemaGapScoring {

    private NcemaGapScoring() {
    }

    public static GapLayerScoreSummary compute(List<GapItem> items, Map<Long, FrameworkNodeResponse> nodesById) {
        Map<Long, FrameworkNodeResponse> byId = nodesById != null ? nodesById : Map.of();
        Map<String, FrameworkNodeResponse> pillarByCode = pillarNodesByCode(byId);

        List<GapItem> applicable = items.stream()
                .filter(i -> i.getApplicability() == GapApplicability.APPLICABLE)
                .toList();

        Map<String, List<GapItem>> byPillar = new LinkedHashMap<>();
        for (GapItem item : applicable) {
            FrameworkNodeResponse pillar = resolvePillar(byId.get(item.getFrameworkNodeId()), byId, pillarByCode);
            if (pillar == null) {
                continue;
            }
            byPillar.computeIfAbsent(pillar.code(), k -> new ArrayList<>()).add(item);
        }

        List<GapLayerScoreSummary.DomainScore> pillars = byPillar.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    FrameworkNodeResponse pillarNode = pillarByCode.get(e.getKey());
                    GapScoring.Result result = GapScoring.compute(e.getValue());
                    return new GapLayerScoreSummary.DomainScore(
                            e.getKey(),
                            pillarNode != null ? pillarNode.title() : e.getKey(),
                            result.overallScore(),
                            result.ragStatus(),
                            result.applicableCount(),
                            result.scoredCount());
                })
                .toList();

        GapLayerScoreSummary.LayerScore overall = toLayerScore(GapScoring.compute(applicable));
        return new GapLayerScoreSummary(null, null, pillars, overall);
    }

    private static Map<String, FrameworkNodeResponse> pillarNodesByCode(Map<Long, FrameworkNodeResponse> byId) {
        Map<String, FrameworkNodeResponse> pillars = new HashMap<>();
        for (FrameworkNodeResponse node : byId.values()) {
            if (node.level() == 1) {
                pillars.put(node.code(), node);
            }
        }
        return pillars;
    }

    private static FrameworkNodeResponse resolvePillar(FrameworkNodeResponse node,
                                                       Map<Long, FrameworkNodeResponse> byId,
                                                       Map<String, FrameworkNodeResponse> pillarByCode) {
        if (node == null) {
            return null;
        }
        if (node.level() == 1 && pillarByCode.containsKey(node.code())) {
            return node;
        }
        if (node.parentId() == null) {
            return null;
        }
        return resolvePillar(byId.get(node.parentId()), byId, pillarByCode);
    }

    private static GapLayerScoreSummary.LayerScore toLayerScore(GapScoring.Result result) {
        if (result == null) {
            return null;
        }
        return new GapLayerScoreSummary.LayerScore(
                result.overallScore(),
                result.ragStatus(),
                result.applicableCount(),
                result.scoredCount());
    }
}
