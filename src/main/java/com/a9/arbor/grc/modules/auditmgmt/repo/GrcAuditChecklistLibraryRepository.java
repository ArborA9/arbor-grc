package com.a9.arbor.grc.modules.auditmgmt.repo;

import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistTemplateKey;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistLibrary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrcAuditChecklistLibraryRepository extends JpaRepository<GrcAuditChecklistLibrary, Long> {
    List<GrcAuditChecklistLibrary> findAllByOrderBySystemSeededDescNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByTemplateKeyAndSystemSeededTrue(ChecklistTemplateKey templateKey);

    Optional<GrcAuditChecklistLibrary> findFirstByTemplateKeyAndSystemSeededTrue(ChecklistTemplateKey templateKey);

    Optional<GrcAuditChecklistLibrary> findFirstByTemplateKeyOrderByIdAsc(ChecklistTemplateKey templateKey);
}
