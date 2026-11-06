package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistTemplateKey;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class AuditChecklistLibrary {

    public record Template(ChecklistTemplateKey key, String name, List<Line> lines) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FilePayload(String key, String name, List<Line> lines) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Line(
            String clauseRef,
            String section,
            String title,
            String testFocus,
            String soaApplicability,
            String evidenceLives
    ) {
    }

    private final Map<ChecklistTemplateKey, Template> templates = new EnumMap<>(ChecklistTemplateKey.class);

    public AuditChecklistLibrary(ObjectMapper objectMapper) {
        load(objectMapper, "audit/checklists/iso-27001.json");
        load(objectMapper, "audit/checklists/iso-20000.json");
        load(objectMapper, "audit/checklists/uae-ia.json");
    }

    public List<Template> all() {
        return List.of(
                require(ChecklistTemplateKey.ISO_27001),
                require(ChecklistTemplateKey.ISO_20000),
                require(ChecklistTemplateKey.UAE_IA));
    }

    public Template require(ChecklistTemplateKey key) {
        Template t = templates.get(key);
        if (t == null) {
            throw new IllegalArgumentException("Unknown checklist template: " + key);
        }
        return t;
    }

    private void load(ObjectMapper objectMapper, String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            FilePayload file = objectMapper.readValue(in, FilePayload.class);
            ChecklistTemplateKey key = ChecklistTemplateKey.valueOf(file.key());
            templates.put(key, new Template(key, file.name(), file.lines() == null ? List.of() : file.lines()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + path, e);
        }
    }
}
