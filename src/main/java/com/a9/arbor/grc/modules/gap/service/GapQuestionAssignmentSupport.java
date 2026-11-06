package com.a9.arbor.grc.modules.gap.service;

import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.domain.GapQuestionAssignmentMode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Serialize / interpret selective Gap question assignment on a control. */
public final class GapQuestionAssignmentSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Long>> LONG_LIST = new TypeReference<>() {
    };

    private GapQuestionAssignmentSupport() {
    }

    public static List<Long> parseIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Long> ids = MAPPER.readValue(json, LONG_LIST);
            if (ids == null) {
                return List.of();
            }
            List<Long> out = new ArrayList<>();
            for (Long id : ids) {
                if (id != null) {
                    out.add(id);
                }
            }
            return List.copyOf(out);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid assigned question id list");
        }
    }

    public static String toJson(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(ids);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not serialize question ids");
        }
    }

    public static GapQuestionAssignmentMode modeOf(GapItem item) {
        return item.getQuestionAssignmentMode() != null
                ? item.getQuestionAssignmentMode()
                : GapQuestionAssignmentMode.ALL;
    }

    public static boolean allowsFrameworkQuestion(GapItem item, long questionId) {
        if (modeOf(item) == GapQuestionAssignmentMode.ALL) {
            return true;
        }
        return parseIds(item.getAssignedFrameworkQuestionIdsJson()).contains(questionId);
    }

    public static boolean allowsCustomQuestion(GapItem item, long customQuestionId) {
        if (modeOf(item) == GapQuestionAssignmentMode.ALL) {
            return true;
        }
        return parseIds(item.getAssignedCustomQuestionIdsJson()).contains(customQuestionId);
    }

    public static void apply(GapItem item, Boolean assignAllQuestions,
                      List<Long> frameworkQuestionIds, List<Long> customQuestionIds) {
        boolean all = assignAllQuestions == null || Boolean.TRUE.equals(assignAllQuestions);
        if (all) {
            item.setQuestionAssignmentMode(GapQuestionAssignmentMode.ALL);
            item.setAssignedFrameworkQuestionIdsJson(null);
            item.setAssignedCustomQuestionIdsJson(null);
            return;
        }
        item.setQuestionAssignmentMode(GapQuestionAssignmentMode.SELECTED);
        List<Long> fw = frameworkQuestionIds != null ? frameworkQuestionIds : List.of();
        List<Long> cu = customQuestionIds != null ? customQuestionIds : List.of();
        // Deduplicate while preserving order
        item.setAssignedFrameworkQuestionIdsJson(toJson(dedupe(fw)));
        item.setAssignedCustomQuestionIdsJson(toJson(dedupe(cu)));
    }

    private static List<Long> dedupe(List<Long> ids) {
        Set<Long> seen = new HashSet<>();
        List<Long> out = new ArrayList<>();
        for (Long id : ids) {
            if (id != null && seen.add(id)) {
                out.add(id);
            }
        }
        return out;
    }

    static List<Long> frameworkIdsOrEmpty(GapItem item) {
        if (modeOf(item) == GapQuestionAssignmentMode.ALL) {
            return Collections.emptyList();
        }
        return parseIds(item.getAssignedFrameworkQuestionIdsJson());
    }

    static List<Long> customIdsOrEmpty(GapItem item) {
        if (modeOf(item) == GapQuestionAssignmentMode.ALL) {
            return Collections.emptyList();
        }
        return parseIds(item.getAssignedCustomQuestionIdsJson());
    }
}
