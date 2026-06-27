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
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("runId", runId);
        payload.put("eventName", eventName);
        payload.put("timestamp", Instant.now().toString());
        if (toolCallId != null && !toolCallId.isBlank()) {
            payload.put("toolCallId", toolCallId);
        }
        if (conversionId != null && !conversionId.isBlank()) {
            payload.put("conversionId", conversionId);
        }
        if (message != null && !message.isBlank()) {
            payload.put("message", message);
        }
        return payload;
    }
}
