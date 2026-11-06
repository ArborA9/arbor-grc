package com.a9.arbor.grc.modules.gap.service;

import com.a9.arbor.grc.modules.gap.web.dto.GapLayerScoreSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Serialize / parse UAE IA three-layer score snapshots on Gap assessments. */
public final class GapLayerScoreJson {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private GapLayerScoreJson() {
    }

    public static String write(GapLayerScoreSummary summary) {
        if (summary == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize layer scores", e);
        }
    }

    public static GapLayerScoreSummary parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, GapLayerScoreSummary.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
