package com.livelynovel.agent;

import com.livelynovel.model.enums.AgentToolSideEffectLevelEnum;

import java.util.Objects;

public record AgentTool(
        String name,
        String description,
        AgentToolSideEffectLevelEnum sideEffectLevel,
        AgentToolExecutor executor
) {
    public AgentTool {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("工具名不能为空");
        }
        Objects.requireNonNull(sideEffectLevel, "sideEffectLevel");
        Objects.requireNonNull(executor, "executor");
    }

    @FunctionalInterface
    public interface AgentToolExecutor {
        AgentToolResult execute(AgentToolContext context);
    }
}
