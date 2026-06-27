package com.livelynovel.model.entity;

import com.livelynovel.model.enums.AgentGuardrailStatusEnum;
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
 * Agent guardrail check result trace record.
 */
@Entity
@Table(name = "agent_guardrail_result")
@Getter
@Setter
public class AgentGuardrailResultEntity {

    @Id
    @Column(length = 32, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 32)
    private String runId;

    @Column(nullable = false, length = 32)
    private String stepId;

    @Column(nullable = false, length = 128)
    private String guardrailName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AgentGuardrailStatusEnum status;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    @Column(nullable = false)
    private Instant createdAt;
}
