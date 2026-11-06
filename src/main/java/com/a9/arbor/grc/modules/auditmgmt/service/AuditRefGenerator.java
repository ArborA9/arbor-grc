package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditRefSeq;
import com.a9.arbor.grc.modules.auditmgmt.repo.AuditRefSeqRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
public class AuditRefGenerator {

    private final AuditRefSeqRepository seqRepository;

    public AuditRefGenerator(AuditRefSeqRepository seqRepository) {
        this.seqRepository = seqRepository;
    }

    @Transactional
    public String nextAuditId() {
        return next("AUD");
    }

    @Transactional
    public String nextGrantId() {
        return next("GR");
    }

    private String next(String kind) {
        int year = Year.now().getValue();
        AuditRefSeq.Pk pk = new AuditRefSeq.Pk();
        pk.setKind(kind);
        pk.setYearNum(year);
        AuditRefSeq seq = seqRepository.findById(pk).orElseGet(() -> {
            AuditRefSeq row = new AuditRefSeq();
            row.setId(pk);
            row.setNextVal(1);
            return seqRepository.save(row);
        });
        int n = seq.getNextVal();
        seq.setNextVal(n + 1);
        seqRepository.save(seq);
        return String.format("%s-%d-%03d", kind, year, n);
    }
}
