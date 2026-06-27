package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AgentTraceDTO {

    private String runId;
    private String userGoal;
    private String novelId;
    private String conversionId;
    private String screenplayType;
    private String status;
    private int currentStepIndex;
    private Instant startedAt;
    private Instant updatedAt;
    private Instant completedAt;
    private String finalArtifactRef;
    private String errorMessage;
    private List<AgentStepTraceDTO> steps = new ArrayList<>();
    private List<AgentToolCallTraceDTO> unassignedToolCalls = new ArrayList<>();
}
