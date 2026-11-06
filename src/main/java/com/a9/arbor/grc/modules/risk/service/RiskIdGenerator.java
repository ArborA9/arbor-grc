package com.a9.arbor.grc.modules.risk.service;

import com.a9.arbor.grc.modules.risk.domain.RiskIdSeq;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.repo.RiskIdSeqRepository;
import com.a9.arbor.grc.modules.risk.repo.RiskRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
public class RiskIdGenerator {

    private final RiskIdSeqRepository seqRepository;
    private final RiskRecordRepository riskRepository;

    public RiskIdGenerator(RiskIdSeqRepository seqRepository, RiskRecordRepository riskRepository) {
        this.seqRepository = seqRepository;
        this.riskRepository = riskRepository;
    }

    @Transactional
    public String nextDisplayId(RiskType type) {
        int year = Year.now().getValue();
        String typeKey = type.name();
        RiskIdSeq.Pk pk = new RiskIdSeq.Pk();
        pk.setRiskType(typeKey);
        pk.setYearNum(year);
        RiskIdSeq seq = seqRepository.findById(pk)
                .orElseGet(() -> {
                    RiskIdSeq row = new RiskIdSeq();
                    row.setRiskType(typeKey);
                    row.setYearNum(year);
                    row.setNextVal(1);
                    return seqRepository.save(row);
                });
        String prefix = switch (type) {
            case IS_RISK -> "ISR";
            case ERM -> "ERM";
            case PRIVACY_RISK -> "DPR";
        };
        int n = seq.getNextVal();
        String id = String.format("%s-%d-%03d", prefix, year, n);
        while (!riskRepository.findDisplayIdsByKey(idKey(id)).isEmpty()) {
            n++;
            id = String.format("%s-%d-%03d", prefix, year, n);
        }
        seq.setNextVal(n + 1);
        seqRepository.save(seq);
        return id;
    }

    /** Comparison key for Risk IDs: case, spaces, dashes and underscores are ignored. */
    public static String idKey(String displayId) {
        return displayId.toUpperCase().replace(" ", "").replace("-", "").replace("_", "");
    }

    @Transactional
    public String nextAcceptanceId() {
        int year = Year.now().getValue();
        String typeKey = "RA";
        RiskIdSeq.Pk pk = new RiskIdSeq.Pk();
        pk.setRiskType(typeKey);
        pk.setYearNum(year);
        RiskIdSeq seq = seqRepository.findById(pk)
                .orElseGet(() -> {
                    RiskIdSeq row = new RiskIdSeq();
                    row.setRiskType(typeKey);
                    row.setYearNum(year);
                    row.setNextVal(1);
                    return seqRepository.save(row);
                });
        int n = seq.getNextVal();
        seq.setNextVal(n + 1);
        seqRepository.save(seq);
        return String.format("RA-%d-%03d", year, n);
    }
}
