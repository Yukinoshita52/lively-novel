package com.livelynovel.agent;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentSseEventFactoryTest {

    @Test
    void createsPayloadWithRequiredFields() {
        AgentSseEventFactory factory = new AgentSseEventFactory();

        Map<String, Object> payload = factory.payload(
                "agent_started",
                "ar-1234abcd",
                null,
                "cv-1234abcd",
                "Agent run started"
        );

        assertThat(payload.get("runId")).isEqualTo("ar-1234abcd");
        assertThat(payload.get("eventName")).isEqualTo("agent_started");
        assertThat(payload.get("timestamp")).isInstanceOf(String.class);
        assertThat(payload.get("conversionId")).isEqualTo("cv-1234abcd");
        assertThat(payload.get("message")).isEqualTo("Agent run started");
    }
}
