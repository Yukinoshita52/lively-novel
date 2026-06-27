package com.livelynovel.agent;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ToolRegistry {

    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public synchronized void register(AgentTool tool) {
        if (tools.containsKey(tool.name())) {
            throw new IllegalArgumentException("工具已注册: " + tool.name());
        }
        tools.put(tool.name(), tool);
    }

    public synchronized AgentTool getRequired(String toolName) {
        AgentTool tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("未知工具: " + toolName);
        }
        return tool;
    }

    public AgentToolResult execute(String toolName, AgentToolContext context) {
        return getRequired(toolName).executor().execute(context);
    }
}
