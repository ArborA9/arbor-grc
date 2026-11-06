package com.a9.arbor.grc.modules.kri.service;

import com.a9.arbor.grc.modules.kri.domain.KriIdSeq;
import com.a9.arbor.grc.modules.kri.repo.KriIdSeqRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Component
public class KriIdGenerator {

    private final KriIdSeqRepository seqRepository;

    public KriIdGenerator(KriIdSeqRepository seqRepository) {
        this.seqRepository = seqRepository;
    }

    @Transactional
    public String nextDisplayId() {
        int year = Year.now().getValue();
        KriIdSeq.Pk pk = new KriIdSeq.Pk(year);
        KriIdSeq seq = seqRepository.findById(pk).orElseGet(() -> {
            KriIdSeq row = new KriIdSeq();
            row.setYearVal(year);
            row.setNextVal(1);
            return row;
        });
        int n = seq.getNextVal();
        seq.setNextVal(n + 1);
        seqRepository.save(seq);
        return "KRI-%d-%03d".formatted(year, n);
    }
}
