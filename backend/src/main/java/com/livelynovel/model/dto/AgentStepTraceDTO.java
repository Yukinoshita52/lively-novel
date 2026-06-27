package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AgentStepTraceDTO {

    private String stepId;
    private int stepIndex;
    private String stepName;
    private String agentName;
    private String status;
    private String inputSummary;
    private String outputSummary;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private List<AgentGuardrailTraceDTO> guardrails = new ArrayList<>();
    private List<AgentToolCallTraceDTO> toolCalls = new ArrayList<>();
}
