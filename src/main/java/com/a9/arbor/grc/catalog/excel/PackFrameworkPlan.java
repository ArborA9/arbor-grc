package com.a9.arbor.grc.catalog.excel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A whole framework derived from a reference pack — nodes, hierarchy and question bank — ready for
 * the same persistence path the Master import uses.
 *
 * <p>Decided before anything is written, so a pack that cannot produce a routed node fails the
 * import rather than leaving a framework with nothing in it.
 *
 * @param info transformations worth reporting: synthesised section codes, disambiguated duplicate
 *             refs, ancestors that had no banner to take a title from
 */
public record PackFrameworkPlan(
        List<ParsedFrameworkRow> nodes,
        List<ParsedMasterWorkbook.Question> questions,
        List<ImportStagingRow> staging,
        List<ImportRowError> errors,
        List<String> info
) {

    public PackFrameworkPlan {
        nodes = List.copyOf(nodes);
        questions = List.copyOf(questions);
        staging = List.copyOf(staging);
        errors = List.copyOf(errors);
        info = List.copyOf(info);
    }

    /** Node count per level, ascending — the shape assertion an import report is checked against. */
    public Map<Integer, Integer> nodesByLevel() {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        nodes.stream()
                .mapToInt(ParsedFrameworkRow::level)
                .sorted()
                .forEach(level -> counts.merge(level, 1, Integer::sum));
        return counts;
    }

    /** Nodes routed to at least one module — what becomes {@code framework.control_count}. */
    public int routedNodes() {
        return (int) nodes.stream().filter(ParsedFrameworkRow::routedToAnyModule).count();
    }
}
