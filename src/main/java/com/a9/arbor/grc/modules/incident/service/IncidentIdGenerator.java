package com.a9.arbor.grc.modules.incident.service;

import com.a9.arbor.grc.modules.incident.domain.IncidentIdSeq;
import com.a9.arbor.grc.modules.incident.repo.IncidentIdSeqRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Component
public class IncidentIdGenerator {

    private final IncidentIdSeqRepository seqRepository;

    public IncidentIdGenerator(IncidentIdSeqRepository seqRepository) {
        this.seqRepository = seqRepository;
    }

    @Transactional
    public String nextDisplayId() {
        int year = Year.now().getValue();
        IncidentIdSeq.Pk pk = new IncidentIdSeq.Pk(year);
        IncidentIdSeq seq = seqRepository.findById(pk).orElseGet(() -> {
            IncidentIdSeq row = new IncidentIdSeq();
            row.setYearVal(year);
            row.setNextVal(1);
            return row;
        });
        int n = seq.getNextVal();
        seq.setNextVal(n + 1);
        seqRepository.save(seq);
        return "INC-%d-%03d".formatted(year, n);
    }
}
