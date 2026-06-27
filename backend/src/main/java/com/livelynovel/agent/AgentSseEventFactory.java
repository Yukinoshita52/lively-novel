package com.livelynovel.agent;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AgentSseEventFactory {

    public Map<String, Object> payload(
            String eventName,
            String runId,
            String toolCallId,
            String conversionId,
            String message
    ) {
        return payload(eventName, runId, null, toolCallId, conversionId, null, null, message);
    }

    public Map<String, Object> payload(
            String eventName,
            String runId,
            String stepId,
            String toolCallId,
            String conversionId,
            String guardrailName,
            String guardrailStatus,
            String message
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("runId", runId);
        payload.put("eventName", eventName);
        payload.put("timestamp", Instant.now().toString());
        putIfPresent(payload, "stepId", stepId);
        putIfPresent(payload, "toolCallId", toolCallId);
        putIfPresent(payload, "conversionId", conversionId);
        putIfPresent(payload, "guardrailName", guardrailName);
        putIfPresent(payload, "guardrailStatus", guardrailStatus);
        putIfPresent(payload, "message", message);
        return payload;
    }

    private void putIfPresent(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.isBlank()) {
            payload.put(key, value);
        }
    }
}
