package com.livelynovel.model.entity;

import com.livelynovel.model.enums.AgentStepStatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Agent step trace record within a run.
 */
@Entity
@Table(name = "agent_step")
@Getter
@Setter
public class AgentStepEntity {

    @Id
    @Column(length = 32, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 32)
    private String runId;

    @Column(nullable = false)
    private int stepIndex;

    @Column(nullable = false, length = 128)
    private String stepName;

    @Column(nullable = false, length = 128)
    private String agentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AgentStepStatusEnum status;

    @Column(columnDefinition = "TEXT")
    private String inputSummary;

    @Column(columnDefinition = "TEXT")
    private String outputSummary;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
