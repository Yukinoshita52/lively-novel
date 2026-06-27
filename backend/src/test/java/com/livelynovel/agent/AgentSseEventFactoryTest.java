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

    @Test
    void createsPayloadWithStepAndGuardrailFields() {
        AgentSseEventFactory factory = new AgentSseEventFactory();

        Map<String, Object> payload = factory.payload(
                "guardrail_checked",
                "ar-1234abcd",
                "step-1234",
                "atc-1234abcd",
                "cv-1234abcd",
                "tool_side_effect_allowed",
                "PASS",
                "Tool side effect WRITE_COST is allowed"
        );

        assertThat(payload.get("runId")).isEqualTo("ar-1234abcd");
        assertThat(payload.get("eventName")).isEqualTo("guardrail_checked");
        assertThat(payload.get("timestamp")).isInstanceOf(String.class);
        assertThat(payload.get("stepId")).isEqualTo("step-1234");
        assertThat(payload.get("toolCallId")).isEqualTo("atc-1234abcd");
        assertThat(payload.get("conversionId")).isEqualTo("cv-1234abcd");
        assertThat(payload.get("guardrailName")).isEqualTo("tool_side_effect_allowed");
        assertThat(payload.get("guardrailStatus")).isEqualTo("PASS");
        assertThat(payload.get("message")).isEqualTo("Tool side effect WRITE_COST is allowed");
    }
}
