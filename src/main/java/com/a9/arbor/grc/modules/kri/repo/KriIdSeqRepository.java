package com.a9.arbor.grc.modules.kri.repo;

import com.a9.arbor.grc.modules.kri.domain.KriIdSeq;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KriIdSeqRepository extends JpaRepository<KriIdSeq, KriIdSeq.Pk> {
}
