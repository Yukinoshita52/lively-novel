package com.livelynovel.repository;

import com.livelynovel.model.entity.AgentToolCallEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Agent tool invocation trace repository.
 */
public interface AgentToolCallRepository extends JpaRepository<AgentToolCallEntity, String> {

    List<AgentToolCallEntity> findByRunIdOrderByStartedAtAsc(String runId);
}
