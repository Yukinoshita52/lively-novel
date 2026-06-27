package com.livelynovel.agent;

import com.livelynovel.model.entity.AgentStepEntity;
import com.livelynovel.model.enums.AgentGuardrailStatusEnum;
import com.livelynovel.model.enums.AgentStepStatusEnum;
import com.livelynovel.model.enums.AgentToolSideEffectLevelEnum;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GuardrailServiceTest {

    private final GuardrailService guardrailService = new GuardrailService();

    @Test
    void passesRegisteredToolWithValidContextAndAllowedSideEffect() {
        AgentTool tool = conversionTool();
        AgentToolContext context = new AgentToolContext(
                "agent-run-1",
                "novel-1",
                ScreenplayTypeEnum.ANIME,
                null
        );

        List<AgentGuardrailCheckResult> results = guardrailService.checkBeforeToolCall(
                tool,
                context,
                step(),
                List.of(AgentToolSideEffectLevelEnum.WRITE_COST)
        );

        assertThat(results).hasSize(3);
        assertThat(results).allMatch(result -> result.status() == AgentGuardrailStatusEnum.PASS);
        assertThat(results).extracting(AgentGuardrailCheckResult::guardrailName)
                .containsExactly("tool_registered", "agent_context_valid", "tool_side_effect_allowed");
    }

    @Test
    void blocksMissingNovelId() {
        AgentToolContext context = new AgentToolContext(
                "agent-run-1",
                " ",
                ScreenplayTypeEnum.ANIME,
                null
        );

        List<AgentGuardrailCheckResult> results = guardrailService.checkBeforeToolCall(
                conversionTool(),
                context,
                step(),
                List.of(AgentToolSideEffectLevelEnum.WRITE_COST)
        );

        assertThat(results).anySatisfy(result -> {
            assertThat(result.guardrailName()).isEqualTo("agent_context_valid");
            assertThat(result.status()).isEqualTo(AgentGuardrailStatusEnum.BLOCK);
            assertThat(result.message()).contains("novelId");
            assertThat(result.blocksExecution()).isTrue();
        });
    }

    @Test
    void blocksDisallowedSideEffect() {
        AgentToolContext context = new AgentToolContext(
                "agent-run-1",
                "novel-1",
                ScreenplayTypeEnum.ANIME,
                null
        );

        List<AgentGuardrailCheckResult> results = guardrailService.checkBeforeToolCall(
                conversionTool(),
                context,
                step(),
                List.of(AgentToolSideEffectLevelEnum.NONE)
        );

        assertThat(results).anySatisfy(result -> {
            assertThat(result.guardrailName()).isEqualTo("tool_side_effect_allowed");
            assertThat(result.status()).isEqualTo(AgentGuardrailStatusEnum.BLOCK);
            assertThat(result.message()).contains("not allowed");
            assertThat(result.blocksExecution()).isTrue();
        });
    }

    private AgentTool conversionTool() {
        return new AgentTool(
                "runExistingScreenplayConversion",
                "复用现有整本剧本转换流程",
                AgentToolSideEffectLevelEnum.WRITE_COST,
                context -> AgentToolResult.completed("conversion-1", "{}")
        );
    }

    private AgentStepEntity step() {
        AgentStepEntity step = new AgentStepEntity();
        step.setId("step-1");
        step.setRunId("agent-run-1");
        step.setStepIndex(0);
        step.setStepName("start_existing_conversion");
        step.setAgentName("ScreenplayConversionAgent");
        step.setStatus(AgentStepStatusEnum.RUNNING);
        step.setStartedAt(Instant.parse("2026-06-27T12:55:00Z"));
        return step;
    }
}
