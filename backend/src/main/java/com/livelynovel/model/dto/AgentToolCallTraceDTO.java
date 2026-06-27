package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class AgentToolCallTraceDTO {

    private String toolCallId;
    private String stepId;
    private String toolName;
    private String sideEffectLevel;
    private String inputJson;
    private String outputJson;
    private String status;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
}
