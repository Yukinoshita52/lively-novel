package com.livelynovel.agent;

public record AgentToolResult(String conversionId, String outputJson) {

    public static AgentToolResult completed(String conversionId, String outputJson) {
        return new AgentToolResult(conversionId, outputJson == null ? "{}" : outputJson);
    }
}
