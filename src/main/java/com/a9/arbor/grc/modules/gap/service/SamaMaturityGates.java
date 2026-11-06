package com.a9.arbor.grc.modules.gap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * SAMA CSF Gap evidence gates (workbook cols M–Q). Cumulative: final maturity is the count of
 * consecutive Yes answers from gate 1; first No stops; leading blank → not assessed (null).
 *
 * <p>Aligned with {@code SAMA_CSF_Gap_Assessment_Maturity_Workbook_2.xlsx} Maturity Model / Gap Assessment.
 */
public final class SamaMaturityGates {

    public static final int GATE_COUNT = 5;

    /** Short labels for contributor / admin UI (workbook gate questions). */
    public static final String[] GATE_LABELS = {
            "Level 1: Is there ANYTHING at all? (documented awareness, activity, or named owner)",
            "Level 2: Is it done consistently across departments/owners (even if informal)?",
            "Level 3: Approved policy + standard + procedure, implemented, compliance monitored, KPIs reported?",
            "Level 4: KRIs with thresholds, effectiveness measured, trend reporting?",
            "Level 5: Continuous improvement, ERM integrated, real-time monitoring, peer benchmarking?",
    };

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SamaMaturityGates() {
    }

    /**
     * @param gates length 5; each {@code true}/{@code false}/{@code null} (unanswered)
     * @return maturity 0–5, or null if gate 1 is unanswered
     */
    public static Integer maturityFromGates(Boolean[] gates) {
        if (gates == null || gates.length != GATE_COUNT) {
            return null;
        }
        if (gates[0] == null) {
            return null;
        }
        if (Boolean.FALSE.equals(gates[0])) {
            return 0;
        }
        // Consecutive Yes from gate 1; first No or blank stops (maturity = count of Yes).
        for (int i = 1; i < GATE_COUNT; i++) {
            if (!Boolean.TRUE.equals(gates[i])) {
                return i;
            }
        }
        return 5;
    }

    /** True when every gate is Yes or No (no blanks): required for contributor submit. */
    public static boolean isComplete(Boolean[] gates) {
        if (gates == null || gates.length != GATE_COUNT) {
            return false;
        }
        for (Boolean g : gates) {
            if (g == null) {
                return false;
            }
        }
        return true;
    }

    public static String toJson(Boolean[] gates) {
        if (gates == null) {
            return null;
        }
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode arr = root.putArray("gates");
        for (int i = 0; i < GATE_COUNT; i++) {
            Boolean g = i < gates.length ? gates[i] : null;
            if (g == null) {
                arr.addNull();
            } else {
                arr.add(g);
            }
        }
        try {
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize SAMA maturity gates", e);
        }
    }

    public static Boolean[] fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new Boolean[GATE_COUNT];
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode arr = root.get("gates");
            Boolean[] out = new Boolean[GATE_COUNT];
            if (arr != null && arr.isArray()) {
                for (int i = 0; i < GATE_COUNT && i < arr.size(); i++) {
                    JsonNode n = arr.get(i);
                    if (n == null || n.isNull()) {
                        out[i] = null;
                    } else {
                        out[i] = n.asBoolean();
                    }
                }
            }
            return out;
        } catch (JsonProcessingException e) {
            return new Boolean[GATE_COUNT];
        }
    }

    public static Boolean[] fromList(java.util.List<Boolean> gates) {
        Boolean[] out = new Boolean[GATE_COUNT];
        if (gates == null) {
            return out;
        }
        for (int i = 0; i < GATE_COUNT && i < gates.size(); i++) {
            out[i] = gates.get(i);
        }
        return out;
    }
}
