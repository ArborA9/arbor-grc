package com.a9.arbor.grc.catalog.excel;

import java.util.Map;

/**
 * One framework node ready to persist. The Master Import layout carries per-node module routing
 * straight from the spreadsheet; the legacy templates carry none, so {@link #legacy} derives it from
 * the historical control heuristic instead.
 *
 * @param attributes free-form columns bound for {@code framework_node_attribute}
 *                   ({@code section_type}, {@code is_mandatory}, {@code cross_ref_*}, {@code notes})
 */
public record ParsedFrameworkRow(
        int excelRow,
        int level,
        String parentCode,
        String code,
        String title,
        String definition,
        String controlType,
        String priority,
        int displayOrder,
        String recordType,
        String officialRef,
        boolean inSoa,
        boolean inGapAssessment,
        boolean inAuditChecklist,
        boolean inRiskRegister,
        Map<String, String> attributes
) {

    public ParsedFrameworkRow {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * Row from a legacy template, which has no routing columns: leaf controls carry definition text,
     * synthetic domain/family parents do not. Keeps {@code ArborFrameworkExcelParser} and
     * {@code GrcChecklistExcelParser} on exactly the behaviour they had before the flags existed.
     */
    public static ParsedFrameworkRow legacy(int excelRow,
                                            int level,
                                            String parentCode,
                                            String code,
                                            String title,
                                            String definition,
                                            String controlType,
                                            String priority,
                                            int displayOrder) {
        boolean control = (definition != null && !definition.isBlank()) || level >= 3;
        return new ParsedFrameworkRow(excelRow, level, parentCode, code, title, definition,
                controlType, priority, displayOrder, null, null,
                control, control, control, control, Map.of());
    }

    /** True when the node is routed to at least one module — the basis for {@code control_count}. */
    public boolean routedToAnyModule() {
        return inSoa || inGapAssessment || inAuditChecklist || inRiskRegister;
    }
}
