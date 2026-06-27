package com.livelynovel.agent;

import com.livelynovel.model.dto.AgentTraceDTO;
import com.livelynovel.model.entity.AgentGuardrailResultEntity;
import com.livelynovel.model.entity.AgentRunEntity;
import com.livelynovel.model.entity.AgentStepEntity;
import com.livelynovel.model.entity.AgentToolCallEntity;
import com.livelynovel.model.enums.AgentGuardrailStatusEnum;
import com.livelynovel.model.enums.AgentRunStatusEnum;
import com.livelynovel.model.enums.AgentStepStatusEnum;
import com.livelynovel.model.enums.AgentToolCallStatusEnum;
import com.livelynovel.model.enums.AgentToolSideEffectLevelEnum;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.repository.AgentGuardrailResultRepository;
import com.livelynovel.repository.AgentRunRepository;
import com.livelynovel.repository.AgentStepRepository;
import com.livelynovel.repository.AgentToolCallRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentTraceServiceTest {

    private final AgentRunRepository runRepository = mock(AgentRunRepository.class);
    private final AgentStepRepository stepRepository = mock(AgentStepRepository.class);
    private final AgentGuardrailResultRepository guardrailResultRepository =
            mock(AgentGuardrailResultRepository.class);
    private final AgentToolCallRepository toolCallRepository = mock(AgentToolCallRepository.class);
    private final AgentTraceService service = new AgentTraceService(
            runRepository,
            stepRepository,
            guardrailResultRepository,
            toolCallRepository
    );

    @Test
    void returnsTraceGroupedByStepAndKeepsUnassignedToolCalls() {
        when(runRepository.findById("ar-1")).thenReturn(Optional.of(run()));
        when(stepRepository.findByRunIdOrderByStepIndexAsc("ar-1")).thenReturn(List.of(step("step-1")));
        when(guardrailResultRepository.findByRunIdOrderByCreatedAtAsc("ar-1"))
                .thenReturn(List.of(guardrail("gr-1", "step-1")));
        when(toolCallRepository.findByRunIdOrderByStartedAtAsc("ar-1"))
                .thenReturn(List.of(toolCall("atc-1", "step-1"), toolCall("atc-2", null)));

        Optional<AgentTraceDTO> trace = service.getTrace("ar-1");

        assertThat(trace).isPresent();
        AgentTraceDTO dto = trace.get();
        assertThat(dto.getRunId()).isEqualTo("ar-1");
        assertThat(dto.getStatus()).isEqualTo("COMPLETED");
        assertThat(dto.getScreenplayType()).isEqualTo("ANIME");
        assertThat(dto.getSteps()).hasSize(1);
        assertThat(dto.getSteps().get(0).getStepId()).isEqualTo("step-1");
        assertThat(dto.getSteps().get(0).getGuardrails()).hasSize(1);
        assertThat(dto.getSteps().get(0).getGuardrails().get(0).getGuardrailResultId()).isEqualTo("gr-1");
        assertThat(dto.getSteps().get(0).getToolCalls()).hasSize(1);
        assertThat(dto.getSteps().get(0).getToolCalls().get(0).getToolCallId()).isEqualTo("atc-1");
        assertThat(dto.getUnassignedToolCalls()).hasSize(1);
        assertThat(dto.getUnassignedToolCalls().get(0).getToolCallId()).isEqualTo("atc-2");
    }

    @Test
    void returnsEmptyWhenRunDoesNotExist() {
        when(runRepository.findById("missing")).thenReturn(Optional.empty());

        Optional<AgentTraceDTO> trace = service.getTrace("missing");

        assertThat(trace).isEmpty();
    }

    private AgentRunEntity run() {
        AgentRunEntity run = new AgentRunEntity();
        run.setId("ar-1");
        run.setUserGoal("将小说转换为动画剧本");
        run.setNovelId("nv-1");
        run.setConversionId("cv-1");
        run.setScreenplayType(ScreenplayTypeEnum.ANIME);
        run.setStatus(AgentRunStatusEnum.COMPLETED);
        run.setCurrentStepIndex(0);
        run.setStartedAt(Instant.parse("2026-06-27T14:00:00Z"));
        run.setUpdatedAt(Instant.parse("2026-06-27T14:00:01Z"));
        run.setCompletedAt(Instant.parse("2026-06-27T14:00:02Z"));
        run.setFinalArtifactRef("cv-1");
        return run;
    }

    private AgentStepEntity step(String id) {
        AgentStepEntity step = new AgentStepEntity();
        step.setId(id);
        step.setRunId("ar-1");
        step.setStepIndex(0);
        step.setStepName("start_existing_conversion");
        step.setAgentName("ScreenplayConversionAgent");
        step.setStatus(AgentStepStatusEnum.COMPLETED);
        step.setInputSummary("{\"novelId\":\"nv-1\"}");
        step.setOutputSummary("{\"conversionId\":\"cv-1\"}");
        step.setStartedAt(Instant.parse("2026-06-27T14:00:00Z"));
        step.setCompletedAt(Instant.parse("2026-06-27T14:00:02Z"));
        return step;
    }

    private AgentGuardrailResultEntity guardrail(String id, String stepId) {
        AgentGuardrailResultEntity result = new AgentGuardrailResultEntity();
        result.setId(id);
        result.setRunId("ar-1");
        result.setStepId(stepId);
        result.setGuardrailName("tool_side_effect_allowed");
        result.setStatus(AgentGuardrailStatusEnum.PASS);
        result.setMessage("Tool side effect WRITE_COST is allowed");
        result.setPayloadJson("{\"sideEffectLevel\":\"WRITE_COST\"}");
        result.setCreatedAt(Instant.parse("2026-06-27T14:00:01Z"));
        return result;
    }

    private AgentToolCallEntity toolCall(String id, String stepId) {
        AgentToolCallEntity toolCall = new AgentToolCallEntity();
        toolCall.setId(id);
        toolCall.setRunId("ar-1");
        toolCall.setStepId(stepId);
        toolCall.setToolName("runExistingScreenplayConversion");
        toolCall.setSideEffectLevel(AgentToolSideEffectLevelEnum.WRITE_COST);
        toolCall.setInputJson("{\"novelId\":\"nv-1\"}");
        toolCall.setOutputJson("{\"conversionId\":\"cv-1\"}");
        toolCall.setStatus(AgentToolCallStatusEnum.COMPLETED);
        toolCall.setStartedAt(Instant.parse("2026-06-27T14:00:01Z"));
        toolCall.setCompletedAt(Instant.parse("2026-06-27T14:00:02Z"));
        return toolCall;
    }
}
