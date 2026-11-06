package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.FrameworkNodeAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FrameworkNodeAttributeRepository extends JpaRepository<FrameworkNodeAttribute, Long> {

    List<FrameworkNodeAttribute> findByNodeIdOrderByAttrKeyAsc(Long nodeId);

    List<FrameworkNodeAttribute> findByNodeIdIn(Collection<Long> nodeIds);

    /** The reference-pack import re-runs against nodes it did not create, so attributes upsert. */
    Optional<FrameworkNodeAttribute> findByNodeIdAndAttrKey(Long nodeId, String attrKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from FrameworkNodeAttribute a where a.nodeId in "
            + "(select n.id from FrameworkNode n where n.frameworkId = :frameworkId)")
    void deleteByFrameworkId(@Param("frameworkId") Long frameworkId);
}
