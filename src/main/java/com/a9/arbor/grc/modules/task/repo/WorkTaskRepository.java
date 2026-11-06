package com.a9.arbor.grc.modules.task.repo;

import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkTaskRepository extends JpaRepository<WorkTask, Long> {
    List<WorkTask> findByAssignedToUserIdAndStatusNotOrderByDueDateAscUpdatedAtDesc(
            Long userId, WorkTaskStatus excluded);

    List<WorkTask> findByAssignedToUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<WorkTask> findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
            String sourceModule, String sourceRecordType, Long sourceRecordId);

    Optional<WorkTask> findBySourceModuleAndSourceRecordTypeAndSourceRecordIdAndAssignedToUserId(
            String sourceModule, String sourceRecordType, Long sourceRecordId, Long assignedToUserId);

    List<WorkTask> findAllBySourceModuleAndSourceRecordTypeAndSourceRecordId(
            String sourceModule, String sourceRecordType, Long sourceRecordId);

    List<WorkTask> findBySourceModuleAndSourceRecordTypeAndSourceRecordIdAndStatusNot(
            String sourceModule, String sourceRecordType, Long sourceRecordId, WorkTaskStatus excluded);

    @Query("""
            SELECT t FROM WorkTask t
            WHERE t.sourceModule = 'GAP'
              AND t.sourceRecordType = 'GAP_ITEM'
              AND t.status IN (
                  com.a9.arbor.grc.modules.task.domain.WorkTaskStatus.PENDING,
                  com.a9.arbor.grc.modules.task.domain.WorkTaskStatus.IN_PROGRESS)
              AND t.dueDate IS NOT NULL
              AND t.dueDate < :asOf
              AND t.notRespondedNotifiedAt IS NULL
            """)
    List<WorkTask> findGapOverdueNotNotified(@Param("asOf") LocalDate asOf);
}
