package com.livelynovel.repository;

import com.livelynovel.model.entity.AgentRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Agent run trace repository.
 */
public interface AgentRunRepository extends JpaRepository<AgentRunEntity, String> {
}
