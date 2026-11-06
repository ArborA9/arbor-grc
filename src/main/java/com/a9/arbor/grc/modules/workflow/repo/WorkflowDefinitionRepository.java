package com.a9.arbor.grc.modules.workflow.repo;

import com.a9.arbor.grc.modules.workflow.domain.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

    List<WorkflowDefinition> findByActiveTrueOrderByCodeAsc();

    Optional<WorkflowDefinition> findFirstByCodeAndActiveTrueOrderByVersionDesc(String code);
}
