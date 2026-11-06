package com.a9.arbor.grc.modules.auditmgmt.repo;

import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistLibraryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GrcAuditChecklistLibraryItemRepository extends JpaRepository<GrcAuditChecklistLibraryItem, Long> {
    List<GrcAuditChecklistLibraryItem> findByLibraryIdOrderBySortOrderAscIdAsc(Long libraryId);

    int countByLibraryId(Long libraryId);

    @Modifying(clearAutomatically = true)
    @Query("delete from GrcAuditChecklistLibraryItem i where i.libraryId = :libraryId")
    void deleteByLibraryId(@Param("libraryId") Long libraryId);
}
