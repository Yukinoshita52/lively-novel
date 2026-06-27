package com.livelynovel.repository;

import com.livelynovel.model.entity.AgentStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Agent step trace repository.
 */
public interface AgentStepRepository extends JpaRepository<AgentStepEntity, String> {

    List<AgentStepEntity> findByRunIdOrderByStepIndexAsc(String runId);
}
