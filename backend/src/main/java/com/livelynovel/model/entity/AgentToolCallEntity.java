package com.livelynovel.model.entity;

import com.livelynovel.model.enums.AgentToolCallStatusEnum;
import com.livelynovel.model.enums.AgentToolSideEffectLevelEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * Agent tool invocation trace record.
 */
@Entity
@Table(name = "agent_tool_call")
@Getter
@Setter
public class AgentToolCallEntity {

    @Id
    @Column(length = 32, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 32)
    private String runId;

    @Column(length = 64)
    private String stepId;

    @Column(nullable = false, length = 128)
    private String toolName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AgentToolSideEffectLevelEnum sideEffectLevel;

    @Column(columnDefinition = "TEXT")
    private String inputJson;

    @Column(columnDefinition = "TEXT")
    private String outputJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AgentToolCallStatusEnum status;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
