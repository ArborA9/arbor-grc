package com.a9.arbor.grc.modules.gap.service;

import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;
import com.a9.arbor.grc.modules.gap.domain.GapApplicability;
import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.web.dto.GapLayerScoreSummary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * UAE IA V2.1 Gap scoring.
 *
 * <p>Control Status % follows {@code GRC_UAE_IA_V2.1.xlsx}: AVERAGEIF of applicable sub-control
 * percentages that already have an implementation status (blank and N/A are left out of that
 * average). A control with no scored sub-control is not started and contributes {@code 0} to
 * rollups.
 *
 * <p>Overall, Management, Technical, and each domain are the equal average of those control
 * Status % values across every applicable control in the set (unstarted = 0). Each control
 * counts once. Domain scores are a breakdown only and are not mixed into Overall. Priority
 * P1-P4 is display-only.
 */
public final class UaeIaGapScoring {

    private UaeIaGapScoring() {
    }

    /** Applicable control Status % and whether any sub-control has been scored. */
    record ControlRollup(BigDecimal statusPercent, boolean started) {
    }

    public static GapLayerScoreSummary compute(List<GapItem> items, Map<Long, FrameworkNodeResponse> nodesById) {
        Map<Long, FrameworkNodeResponse> byId = nodesById != null ? nodesById : Map.of();
        Map<String, FrameworkNodeResponse> domainByCode = domainNodesByCode(byId);

        Map<Long, List<GapItem>> byControl = groupByControl(items, byId);
        Map<Long, ControlRollup> controlResults = new LinkedHashMap<>();
        for (Map.Entry<Long, List<GapItem>> entry : byControl.entrySet()) {
            ControlRollup rollup = rollupControl(entry.getValue());
            if (rollup != null) {
                controlResults.put(entry.getKey(), rollup);
            }
        }

        List<Long> managementControls = controlResults.keySet().stream()
                .filter(id -> "MANAGEMENT".equals(classifyLayer(byId.get(id))))
                .toList();
        List<Long> technicalControls = controlResults.keySet().stream()
                .filter(id -> "TECHNICAL".equals(classifyLayer(byId.get(id))))
                .toList();

        GapLayerScoreSummary.LayerScore management = averageControlLayer(managementControls, controlResults);
        GapLayerScoreSummary.LayerScore technical = averageControlLayer(technicalControls, controlResults);

        Map<String, List<Long>> controlsByDomain = new LinkedHashMap<>();
        for (Long controlId : controlResults.keySet()) {
            FrameworkNodeResponse domain = resolveDomain(byId.get(controlId), byId, domainByCode);
            if (domain == null) {
                continue;
            }
            controlsByDomain.computeIfAbsent(domain.code(), k -> new ArrayList<>()).add(controlId);
        }

        List<GapLayerScoreSummary.DomainScore> domains = controlsByDomain.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    FrameworkNodeResponse domainNode = domainByCode.get(e.getKey());
                    GapLayerScoreSummary.LayerScore layer = averageControlLayer(e.getValue(), controlResults);
                    return new GapLayerScoreSummary.DomainScore(
                            e.getKey(),
                            domainNode != null ? domainNode.title() : e.getKey(),
                            layer != null ? layer.score() : null,
                            layer != null ? layer.rag() : null,
                            layer != null ? layer.applicableCount() : 0,
                            layer != null ? layer.scoredCount() : 0);
                })
                .toList();

        GapLayerScoreSummary.LayerScore overall = averageControlLayer(
                new ArrayList<>(controlResults.keySet()), controlResults);
        return new GapLayerScoreSummary(management, technical, domains, overall);
    }

    /**
     * Node to persist as a Gap row: depth 4 keeps the GAP-routed leaf; depth 3 walks to the L3 control.
     */
    public static FrameworkNodeResponse seedNode(FrameworkNodeResponse gapNode,
                                                 Map<Long, FrameworkNodeResponse> byId,
                                                 int depth) {
        if (gapNode == null || depth >= 4) {
            return gapNode;
        }
        Long id = controlNodeId(gapNode, byId);
        if (id == null) {
            return gapNode;
        }
        FrameworkNodeResponse control = byId.get(id);
        return control != null ? control : gapNode;
    }

    /**
     * Group Gap leaves under their control (L3). Sub-controls (L4+) use {@code parentId};
     * legacy control-level Gap items stay keyed by their own node id.
     */
    static Map<Long, List<GapItem>> groupByControl(List<GapItem> items,
                                                   Map<Long, FrameworkNodeResponse> byId) {
        Map<Long, List<GapItem>> byControl = new LinkedHashMap<>();
        for (GapItem item : items) {
            Long controlId = controlNodeId(byId.get(item.getFrameworkNodeId()), byId);
            if (controlId == null) {
                controlId = item.getFrameworkNodeId();
            }
            byControl.computeIfAbsent(controlId, k -> new ArrayList<>()).add(item);
        }
        return byControl;
    }

    private static Long controlNodeId(FrameworkNodeResponse node, Map<Long, FrameworkNodeResponse> byId) {
        if (node == null) {
            return null;
        }
        if (node.level() <= 3) {
            return node.id();
        }
        FrameworkNodeResponse current = node;
        while (current != null && current.level() > 3) {
            Long parentId = current.parentId();
            current = parentId != null ? byId.get(parentId) : null;
        }
        return current != null ? current.id() : node.parentId();
    }

    /**
     * Excel Status % for one L3: average of scored applicable L4s (AVERAGEIF). Not started = 0.
     * {@code null} when the control has no applicable leaves (out of scope).
     */
    static ControlRollup rollupControl(List<GapItem> leaves) {
        int applicableLeaves = 0;
        List<Integer> scoredPct = new ArrayList<>();
        for (GapItem item : leaves) {
            if (item.getApplicability() != GapApplicability.APPLICABLE) {
                continue;
            }
            applicableLeaves++;
            GapImplementationStatus status = item.effectiveImplementationStatus();
            Integer pct = GapScoring.scoreOf(status);
            if (status != null && pct != null) {
                scoredPct.add(pct);
            }
        }
        if (applicableLeaves == 0) {
            return null;
        }
        if (scoredPct.isEmpty()) {
            return new ControlRollup(BigDecimal.ZERO.setScale(2), false);
        }
        BigDecimal sum = scoredPct.stream().map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(scoredPct.size()), 2, RoundingMode.HALF_UP);
        return new ControlRollup(avg, true);
    }

    private static GapLayerScoreSummary.LayerScore averageControlLayer(List<Long> controlIds,
                                                                       Map<Long, ControlRollup> controlResults) {
        if (controlIds.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        int applicable = 0;
        int scored = 0;
        for (Long id : controlIds) {
            ControlRollup rollup = controlResults.get(id);
            if (rollup == null || rollup.statusPercent() == null) {
                continue;
            }
            applicable++;
            if (rollup.started()) {
                scored++;
            }
            sum = sum.add(rollup.statusPercent());
        }
        if (applicable == 0) {
            return new GapLayerScoreSummary.LayerScore(null, null, 0, 0);
        }
        BigDecimal avg = sum.divide(BigDecimal.valueOf(applicable), 2, RoundingMode.HALF_UP);
        String rag = scored == 0 ? null : GapScoring.ragFor(avg);
        return new GapLayerScoreSummary.LayerScore(avg, rag, applicable, scored);
    }

    private static Map<String, FrameworkNodeResponse> domainNodesByCode(Map<Long, FrameworkNodeResponse> byId) {
        Map<String, FrameworkNodeResponse> domains = new HashMap<>();
        for (FrameworkNodeResponse node : byId.values()) {
            if (node.level() == 1) {
                domains.put(node.code(), node);
            }
        }
        return domains;
    }

    private static FrameworkNodeResponse resolveDomain(FrameworkNodeResponse node,
                                                       Map<Long, FrameworkNodeResponse> byId,
                                                       Map<String, FrameworkNodeResponse> domainByCode) {
        if (node == null) {
            return null;
        }
        FrameworkNodeResponse current = node;
        while (current != null) {
            if (current.level() == 1 || domainByCode.containsKey(current.code())) {
                return domainByCode.getOrDefault(current.code(), current);
            }
            Long parentId = current.parentId();
            current = parentId != null ? byId.get(parentId) : null;
        }
        return null;
    }

    static String classifyLayer(FrameworkNodeResponse node) {
        if (node == null) {
            return null;
        }
        String controlType = node.controlType();
        if (controlType != null && !controlType.isBlank()) {
            String lower = controlType.toLowerCase(Locale.ROOT);
            if (lower.contains("management")) {
                return "MANAGEMENT";
            }
            if (lower.contains("technical")) {
                return "TECHNICAL";
            }
        }
        String code = node.code();
        if (code == null || code.isBlank()) {
            return null;
        }
        char first = Character.toUpperCase(code.charAt(0));
        if (first == 'M') {
            return "MANAGEMENT";
        }
        if (first == 'T') {
            return "TECHNICAL";
        }
        return null;
    }
}
