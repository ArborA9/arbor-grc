package com.a9.arbor.grc.modules.kri.repo;

import com.a9.arbor.grc.modules.kri.domain.KriReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KriReadingRepository extends JpaRepository<KriReading, Long> {
    List<KriReading> findByKriIdOrderByRecordedAtDescIdDesc(long kriId);
}
