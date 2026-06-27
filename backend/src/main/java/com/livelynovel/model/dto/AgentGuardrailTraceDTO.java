package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class AgentGuardrailTraceDTO {

    private String guardrailResultId;
    private String guardrailName;
    private String status;
    private String message;
    private String payloadJson;
    private Instant createdAt;
}
