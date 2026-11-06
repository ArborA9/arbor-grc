package com.a9.arbor.grc.modules.privacy.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class DpiaSeedData {

    private final ObjectMapper objectMapper;
    private volatile SeedFile cached;

    public DpiaSeedData(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SeedFile load() {
        SeedFile local = cached;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (cached == null) {
                try (InputStream in = new ClassPathResource("privacy/dpia_seed_v3.json").getInputStream()) {
                    cached = objectMapper.readValue(in, SeedFile.class);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to load DPIA seed data", e);
                }
            }
            return cached;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedFile(List<SeedQuestion> questions, List<SeedPrinciple> principles) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedQuestion(
            String code,
            String section,
            String principle,
            String question,
            String guidance,
            String responseType,
            List<String> options) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedPrinciple(int sortOrder, String principle, String description) {
    }
}
