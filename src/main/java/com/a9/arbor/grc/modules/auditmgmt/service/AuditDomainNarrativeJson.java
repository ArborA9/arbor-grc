package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditDomainNarrativeResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/** Domain / pillar narratives for UAE IA / NCEMA assessment audits. */
public final class AuditDomainNarrativeJson {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private AuditDomainNarrativeJson() {
    }

    public static String write(List<AuditDomainNarrativeResponse> narratives) {
        if (narratives == null || narratives.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(narratives);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize domain narratives", e);
        }
    }

    public static List<AuditDomainNarrativeResponse> parse(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            AuditDomainNarrativeResponse[] arr =
                    MAPPER.readValue(json, AuditDomainNarrativeResponse[].class);
            return List.of(arr);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    public static List<AuditDomainNarrativeResponse> mergeNarratives(
            List<AuditDomainNarrativeResponse> existing,
            List<AuditDomainNarrativeResponse> updates) {
        if (updates == null || updates.isEmpty()) {
            return existing != null ? existing : List.of();
        }
        var byCode = new java.util.LinkedHashMap<String, AuditDomainNarrativeResponse>();
        if (existing != null) {
            for (AuditDomainNarrativeResponse n : existing) {
                byCode.put(n.code(), n);
            }
        }
        for (AuditDomainNarrativeResponse u : updates) {
            AuditDomainNarrativeResponse prev = byCode.get(u.code());
            byCode.put(u.code(), new AuditDomainNarrativeResponse(
                    u.code(),
                    prev != null ? prev.name() : u.name(),
                    u.narrative()));
        }
        return new ArrayList<>(byCode.values());
    }
}
