package com.a9.arbor.grc.modules.workflow.repo;

import com.a9.arbor.grc.modules.workflow.domain.WorkflowTransitionDef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionDefRepository extends JpaRepository<WorkflowTransitionDef, Long> {

    List<WorkflowTransitionDef> findByDefinitionId(Long definitionId);

    Optional<WorkflowTransitionDef> findByDefinitionIdAndFromStateAndActionCode(
            Long definitionId, String fromState, String actionCode);
}
