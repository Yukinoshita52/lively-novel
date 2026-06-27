package com.livelynovel.repository;

import com.livelynovel.model.entity.AgentGuardrailResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Agent guardrail result trace repository.
 */
public interface AgentGuardrailResultRepository extends JpaRepository<AgentGuardrailResultEntity, String> {

    List<AgentGuardrailResultEntity> findByRunIdOrderByCreatedAtAsc(String runId);

    List<AgentGuardrailResultEntity> findByStepIdOrderByCreatedAtAsc(String stepId);
}
