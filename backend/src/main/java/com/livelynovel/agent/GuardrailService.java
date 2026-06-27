package com.livelynovel.agent;

import com.livelynovel.model.entity.AgentStepEntity;
import com.livelynovel.model.enums.AgentGuardrailStatusEnum;
import com.livelynovel.model.enums.AgentToolSideEffectLevelEnum;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GuardrailService {

    public List<AgentGuardrailCheckResult> checkBeforeToolCall(
            AgentTool tool,
            AgentToolContext context,
            AgentStepEntity step,
            List<AgentToolSideEffectLevelEnum> allowedSideEffects
    ) {
        List<AgentGuardrailCheckResult> results = new ArrayList<>();
        results.add(checkToolRegistered(tool));
        results.add(checkContext(context));
        results.add(checkSideEffect(tool, allowedSideEffects));
        return results;
    }

    private AgentGuardrailCheckResult checkToolRegistered(AgentTool tool) {
        if (tool == null) {
            return block("tool_registered", "Tool is not registered", "{}");
        }
        return pass(
                "tool_registered",
                "Tool " + tool.name() + " is registered",
                "{\"toolName\":\"" + tool.name() + "\"}"
        );
    }

    private AgentGuardrailCheckResult checkContext(AgentToolContext context) {
        if (context == null) {
            return block("agent_context_valid", "Agent context is missing", "{}");
        }
        if (context.novelId() == null || context.novelId().isBlank()) {
            return block("agent_context_valid", "novelId is required", "{}");
        }
        if (context.screenplayType() == null) {
            return block(
                    "agent_context_valid",
                    "screenplayType is required",
                    "{\"novelId\":\"" + context.novelId() + "\"}"
            );
        }
        return pass(
                "agent_context_valid",
                "Agent context is valid",
                "{\"novelId\":\"" + context.novelId() + "\",\"screenplayType\":\""
                        + context.screenplayType() + "\"}"
        );
    }

    private AgentGuardrailCheckResult checkSideEffect(
            AgentTool tool,
            List<AgentToolSideEffectLevelEnum> allowedSideEffects
    ) {
        if (tool == null) {
            return block(
                    "tool_side_effect_allowed",
                    "Tool side effect cannot be checked because tool is missing",
                    "{}"
            );
        }
        if (allowedSideEffects == null || !allowedSideEffects.contains(tool.sideEffectLevel())) {
            return block(
                    "tool_side_effect_allowed",
                    "Tool side effect " + tool.sideEffectLevel() + " is not allowed",
                    "{\"sideEffectLevel\":\"" + tool.sideEffectLevel() + "\"}"
            );
        }
        return pass(
                "tool_side_effect_allowed",
                "Tool side effect " + tool.sideEffectLevel() + " is allowed",
                "{\"sideEffectLevel\":\"" + tool.sideEffectLevel() + "\"}"
        );
    }

    private AgentGuardrailCheckResult pass(String name, String message, String payloadJson) {
        return new AgentGuardrailCheckResult(name, AgentGuardrailStatusEnum.PASS, message, payloadJson);
    }

    private AgentGuardrailCheckResult block(String name, String message, String payloadJson) {
        return new AgentGuardrailCheckResult(name, AgentGuardrailStatusEnum.BLOCK, message, payloadJson);
    }
}
