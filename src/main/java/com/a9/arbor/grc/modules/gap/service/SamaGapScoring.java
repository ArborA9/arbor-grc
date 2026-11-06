package com.a9.arbor.grc.modules.gap.service;

import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;
import com.a9.arbor.grc.modules.gap.domain.GapApplicability;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.web.dto.GapLayerScoreSummary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SAMA CSF Gap scoring: maturity 0–5 (target always 3). Leaf score = maturity × 20.
 * Domain (L1) and subdomain (L2) rollups average leaf %.
 */
public final class SamaGapScoring {

    public static final int TARGET_MATURITY = 3;
    public static final int MAX_MATURITY = 5;

    private SamaGapScoring() {
    }

    public static Integer scoreOf(Integer maturity) {
        if (maturity == null) {
            return null;
        }
        if (maturity < 0 || maturity > MAX_MATURITY) {
            return null;
        }
        return maturity * 20;
    }

    public static void requireValid(Integer maturity, String field) {
        if (maturity == null) {
            return;
        }
        if (maturity < 0 || maturity > MAX_MATURITY) {
            throw new IllegalArgumentException(field + " must be between 0 and " + MAX_MATURITY);
        }
    }

    public static GapLayerScoreSummary compute(List<GapItem> items, Map<Long, FrameworkNodeResponse> nodesById) {
        Map<Long, FrameworkNodeResponse> byId = nodesById != null ? nodesById : Map.of();
        Map<String, FrameworkNodeResponse> domainByCode = nodesAtLevel(byId, 1);
        Map<String, FrameworkNodeResponse> subdomainByCode = nodesAtLevel(byId, 2);

        List<GapItem> applicable = items.stream()
                .filter(i -> i.getApplicability() == GapApplicability.APPLICABLE)
                .toList();

        Map<String, List<GapItem>> byDomain = new LinkedHashMap<>();
        Map<String, List<GapItem>> bySubdomain = new LinkedHashMap<>();
        for (GapItem item : applicable) {
            FrameworkNodeResponse node = byId.get(item.getFrameworkNodeId());
            FrameworkNodeResponse domain = resolveAncestor(node, byId, 1);
            FrameworkNodeResponse subdomain = resolveAncestor(node, byId, 2);
            if (domain != null) {
                byDomain.computeIfAbsent(domain.code(), k -> new ArrayList<>()).add(item);
            }
            if (subdomain != null) {
                bySubdomain.computeIfAbsent(subdomain.code(), k -> new ArrayList<>()).add(item);
            }
        }

        List<GapLayerScoreSummary.DomainScore> domainScores = toDomainScores(byDomain, domainByCode);
        List<GapLayerScoreSummary.DomainScore> subdomainScores = toDomainScores(bySubdomain, subdomainByCode);
        // Domains first, then subdomains (UI lists under layerScores.domains).
        List<GapLayerScoreSummary.DomainScore> combined = new ArrayList<>(domainScores.size() + subdomainScores.size());
        combined.addAll(domainScores);
        combined.addAll(subdomainScores);

        GapScoring.Result overall = maturityAverage(applicable);
        return new GapLayerScoreSummary(null, null, combined, toLayerScore(overall));
    }

    private static List<GapLayerScoreSummary.DomainScore> toDomainScores(
            Map<String, List<GapItem>> grouped,
            Map<String, FrameworkNodeResponse> nodesByCode) {
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    FrameworkNodeResponse node = nodesByCode.get(e.getKey());
                    GapScoring.Result result = maturityAverage(e.getValue());
                    return new GapLayerScoreSummary.DomainScore(
                            e.getKey(),
                            node != null ? node.title() : e.getKey(),
                            result.overallScore(),
                            result.ragStatus(),
                            result.applicableCount(),
                            result.scoredCount());
                })
                .toList();
    }

    /** Average of leaf maturity→% scores (same shape as {@link GapScoring#compute}). */
    static GapScoring.Result maturityAverage(List<GapItem> items) {
        int applicable = 0;
        int scored = 0;
        int sum = 0;
        for (GapItem item : items) {
            if (item.getApplicability() != GapApplicability.APPLICABLE) {
                continue;
            }
            applicable++;
            Integer pct = scoreOf(item.effectiveMaturity());
            if (pct != null) {
                scored++;
                sum += pct;
            }
        }
        BigDecimal overall = null;
        String rag = null;
        if (applicable > 0 && scored > 0) {
            overall = BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(applicable * 100L), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            rag = GapScoring.ragFor(overall);
        }
        return new GapScoring.Result(applicable, scored, overall, rag);
    }

    private static Map<String, FrameworkNodeResponse> nodesAtLevel(Map<Long, FrameworkNodeResponse> byId, int level) {
        Map<String, FrameworkNodeResponse> out = new HashMap<>();
        for (FrameworkNodeResponse node : byId.values()) {
            if (node.level() == level) {
                out.put(node.code(), node);
            }
        }
        return out;
    }

    private static FrameworkNodeResponse resolveAncestor(FrameworkNodeResponse node,
                                                         Map<Long, FrameworkNodeResponse> byId,
                                                         int level) {
        FrameworkNodeResponse current = node;
        while (current != null) {
            if (current.level() == level) {
                return current;
            }
            Long parentId = current.parentId();
            current = parentId != null ? byId.get(parentId) : null;
        }
        return null;
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
