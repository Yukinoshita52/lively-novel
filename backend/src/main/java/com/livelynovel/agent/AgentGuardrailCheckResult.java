package com.livelynovel.agent;

import com.livelynovel.model.enums.AgentGuardrailStatusEnum;

public record AgentGuardrailCheckResult(
        String guardrailName,
        AgentGuardrailStatusEnum status,
        String message,
        String payloadJson
) {

    public boolean blocksExecution() {
        return status == AgentGuardrailStatusEnum.BLOCK;
    }
}
