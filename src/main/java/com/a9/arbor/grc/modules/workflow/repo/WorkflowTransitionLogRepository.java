package com.a9.arbor.grc.modules.workflow.repo;

import com.a9.arbor.grc.modules.workflow.domain.WorkflowTransitionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowTransitionLogRepository extends JpaRepository<WorkflowTransitionLog, Long> {

    List<WorkflowTransitionLog> findByInstanceIdOrderByCreatedAtAsc(Long instanceId);
}
