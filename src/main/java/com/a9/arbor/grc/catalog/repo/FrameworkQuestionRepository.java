package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.FrameworkQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FrameworkQuestionRepository extends JpaRepository<FrameworkQuestion, Long> {

    List<FrameworkQuestion> findByFrameworkIdAndActiveTrueOrderByDisplayOrderAscQuestionCodeAsc(Long frameworkId);

    List<FrameworkQuestion> findByFrameworkIdAndVersionNumberAndActiveTrueOrderByDisplayOrderAsc(
            Long frameworkId, int versionNumber);

    @Query("select coalesce(max(q.versionNumber), 0) from FrameworkQuestion q where q.frameworkId = :frameworkId")
    int maxVersion(Long frameworkId);

    long countByFrameworkIdAndActiveTrue(Long frameworkId);

    List<FrameworkQuestion> findByFrameworkIdAndFrameworkNodeIdAndActiveTrueOrderByDisplayOrderAsc(
            Long frameworkId, Long frameworkNodeId);

    /**
     * Questions are foreign-keyed to {@code framework_node}, so re-importing a framework's nodes
     * removes them anyway via {@code fk_fq_node ON DELETE CASCADE}. Doing it explicitly first keeps
     * the persistence context in step with the database.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from FrameworkQuestion q where q.frameworkId = :frameworkId")
    void deleteByFrameworkId(@Param("frameworkId") Long frameworkId);
}
