package com.a9.arbor.grc.modules.workflow.repo;

import com.a9.arbor.grc.modules.workflow.domain.WorkflowInstance;
import com.a9.arbor.grc.modules.workflow.domain.WorkflowInstanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

    Page<WorkflowInstance> findByStatusOrderByStartedAtDesc(WorkflowInstanceStatus status, Pageable pageable);

    Page<WorkflowInstance> findAllByOrderByStartedAtDesc(Pageable pageable);

    List<WorkflowInstance> findByModuleCodeAndRecordTypeAndRecordIdAndStatus(
            String moduleCode, String recordType, String recordId, WorkflowInstanceStatus status);

    Optional<WorkflowInstance> findByIdAndStatus(Long id, WorkflowInstanceStatus status);
}
