package com.a9.arbor.grc.modules.tprm.service;

import com.a9.arbor.grc.modules.tprm.domain.TprmControl;
import com.a9.arbor.grc.modules.tprm.repo.TprmControlRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

@Service
public class TprmControlLibrarySeeder {

    private final TprmControlRepository controlRepository;
    private final ObjectMapper objectMapper;

    public TprmControlLibrarySeeder(TprmControlRepository controlRepository, ObjectMapper objectMapper) {
        this.controlRepository = controlRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void seedIfEmpty() {
        if (controlRepository.count() > 0) {
            return;
        }
        try (InputStream in = new ClassPathResource("tprm/question-library.json").getInputStream()) {
            List<SeedRow> rows = objectMapper.readValue(in, new TypeReference<>() {
            });
            for (SeedRow row : rows) {
                TprmControl c = new TprmControl();
                c.setControlCode(row.controlId());
                c.setDomain(row.domain());
                c.setRequirement(row.requirement());
                c.setDescription(row.description());
                c.setRequiredEvidence(row.requiredEvidence());
                c.setWeight(row.weight());
                c.setWeightLabel(row.weightLabel());
                c.setMandatory(row.mandatory());
                c.setAskedFromTier(row.askedFromTier());
                c.setIso27001(row.iso27001());
                c.setUaeIa(row.uaeIa());
                c.setOtherStandard(row.otherStandard());
                c.setModule(row.module());
                c.setSortOrder(row.sortOrder());
                controlRepository.save(c);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to seed TPRM question library", e);
        }
    }

    private record SeedRow(
            String controlId,
            String domain,
            String requirement,
            String description,
            String requiredEvidence,
            int weight,
            String weightLabel,
            boolean mandatory,
            int askedFromTier,
            String iso27001,
            String uaeIa,
            String otherStandard,
            int sortOrder,
            String module
    ) {
    }
}
