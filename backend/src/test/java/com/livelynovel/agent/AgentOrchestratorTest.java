package com.livelynovel.agent;

import com.livelynovel.model.entity.AgentGuardrailResultEntity;
import com.livelynovel.model.entity.AgentRunEntity;
import com.livelynovel.model.entity.AgentStepEntity;
import com.livelynovel.model.entity.AgentToolCallEntity;
import com.livelynovel.model.enums.AgentGuardrailStatusEnum;
import com.livelynovel.model.enums.AgentRunStatusEnum;
import com.livelynovel.model.enums.AgentStepStatusEnum;
import com.livelynovel.model.enums.AgentToolSideEffectLevelEnum;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.repository.AgentGuardrailResultRepository;
import com.livelynovel.repository.AgentRunRepository;
import com.livelynovel.repository.AgentStepRepository;
import com.livelynovel.repository.AgentToolCallRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentOrchestratorTest {

    private final AgentRunRepository runRepository = mock(AgentRunRepository.class);
    private final AgentToolCallRepository toolCallRepository = mock(AgentToolCallRepository.class);
    private final AgentStepRepository stepRepository = mock(AgentStepRepository.class);
    private final AgentGuardrailResultRepository guardrailResultRepository =
            mock(AgentGuardrailResultRepository.class);
    private final GuardrailService guardrailService = mock(GuardrailService.class);
    private final AgentSseEventFactory eventFactory = new AgentSseEventFactory();

    @Test
    void executesFixedPlanAndPersistsStepGuardrailAndToolTrace() {
        List<AgentRunStatusEnum> savedRunStatuses = new ArrayList<>();
        List<AgentStepStatusEnum> savedStepStatuses = new ArrayList<>();
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new AgentTool(
                "runExistingScreenplayConversion",
                "复用现有整本剧本转换流程",
                AgentToolSideEffectLevelEnum.WRITE_COST,
                context -> AgentToolResult.completed("cv-1234abcd", "{\"conversionId\":\"cv-1234abcd\"}")
        ));
        when(runRepository.save(any(AgentRunEntity.class))).thenAnswer(invocation -> {
            AgentRunEntity run = invocation.getArgument(0);
            savedRunStatuses.add(run.getStatus());
            return run;
        });
        when(stepRepository.save(any(AgentStepEntity.class))).thenAnswer(invocation -> {
            AgentStepEntity step = invocation.getArgument(0);
            savedStepStatuses.add(step.getStatus());
            return step;
        });
        when(toolCallRepository.save(any(AgentToolCallEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(guardrailResultRepository.save(any(AgentGuardrailResultEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(guardrailService.checkBeforeToolCall(any(), any(), any(), any()))
                .thenReturn(passResults());

        AgentOrchestrator orchestrator = new AgentOrchestrator(
                runRepository,
                toolCallRepository,
                stepRepository,
                guardrailResultRepository,
                toolRegistry,
                guardrailService,
                eventFactory
        );

        SseEmitter emitter = orchestrator.convertNovel("nv-1234abcd", ScreenplayTypeEnum.ANIME);

        assertThat(emitter).isNotNull();
        ArgumentCaptor<AgentRunEntity> runCaptor = ArgumentCaptor.forClass(AgentRunEntity.class);
        verify(runRepository, times(2)).save(runCaptor.capture());
        assertThat(savedRunStatuses).containsExactly(AgentRunStatusEnum.RUNNING, AgentRunStatusEnum.COMPLETED);
        assertThat(runCaptor.getAllValues().get(1).getStatus()).isEqualTo(AgentRunStatusEnum.COMPLETED);
        assertThat(runCaptor.getAllValues().get(1).getConversionId()).isEqualTo("cv-1234abcd");

        ArgumentCaptor<AgentStepEntity> stepCaptor = ArgumentCaptor.forClass(AgentStepEntity.class);
        verify(stepRepository, times(2)).save(stepCaptor.capture());
        assertThat(savedStepStatuses).containsExactly(AgentStepStatusEnum.RUNNING, AgentStepStatusEnum.COMPLETED);
        assertThat(stepCaptor.getAllValues().get(0).getStepName()).isEqualTo("start_existing_conversion");
        assertThat(stepCaptor.getAllValues().get(1).getOutputSummary()).contains("cv-1234abcd");

        verify(guardrailResultRepository, times(3)).save(any(AgentGuardrailResultEntity.class));

        ArgumentCaptor<AgentToolCallEntity> toolCallCaptor = ArgumentCaptor.forClass(AgentToolCallEntity.class);
        verify(toolCallRepository, times(2)).save(toolCallCaptor.capture());
        assertThat(toolCallCaptor.getAllValues().get(0).getToolName())
                .isEqualTo("runExistingScreenplayConversion");
        assertThat(toolCallCaptor.getAllValues().get(0).getStepId()).startsWith("step-");
        assertThat(toolCallCaptor.getAllValues().get(1).getOutputJson())
                .contains("cv-1234abcd");
    }

    @Test
    void blocksToolExecutionWhenGuardrailBlocks() {
        ToolRegistry toolRegistry = new ToolRegistry();
        AgentTool.AgentToolExecutor executor = mock(AgentTool.AgentToolExecutor.class);
        toolRegistry.register(new AgentTool(
                "runExistingScreenplayConversion",
                "复用现有整本剧本转换流程",
                AgentToolSideEffectLevelEnum.WRITE_COST,
                executor
        ));
        when(runRepository.save(any(AgentRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRepository.save(any(AgentStepEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(guardrailResultRepository.save(any(AgentGuardrailResultEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(guardrailService.checkBeforeToolCall(any(), any(), any(), any()))
                .thenReturn(List.of(new AgentGuardrailCheckResult(
                        "agent_context_valid",
                        AgentGuardrailStatusEnum.BLOCK,
                        "novelId is required",
                        "{}"
                )));

        AgentOrchestrator orchestrator = new AgentOrchestrator(
                runRepository,
                toolCallRepository,
                stepRepository,
                guardrailResultRepository,
                toolRegistry,
                guardrailService,
                eventFactory
        );

        orchestrator.convertNovel("nv-1234abcd", ScreenplayTypeEnum.ANIME);

        verify(executor, never()).execute(any());
        verify(toolCallRepository, never()).save(any());
        ArgumentCaptor<AgentRunEntity> runCaptor = ArgumentCaptor.forClass(AgentRunEntity.class);
        verify(runRepository, times(2)).save(runCaptor.capture());
        assertThat(runCaptor.getAllValues().get(1).getStatus()).isEqualTo(AgentRunStatusEnum.FAILED);
        assertThat(runCaptor.getAllValues().get(1).getErrorMessage()).contains("novelId is required");

        ArgumentCaptor<AgentStepEntity> stepCaptor = ArgumentCaptor.forClass(AgentStepEntity.class);
        verify(stepRepository, times(2)).save(stepCaptor.capture());
        assertThat(stepCaptor.getAllValues().get(1).getStatus()).isEqualTo(AgentStepStatusEnum.BLOCKED);
        assertThat(stepCaptor.getAllValues().get(1).getErrorMessage()).contains("novelId is required");
    }

    @Test
    void marksRunAndStepFailedWhenToolFails() {
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new AgentTool(
                "runExistingScreenplayConversion",
                "复用现有整本剧本转换流程",
                AgentToolSideEffectLevelEnum.WRITE_COST,
                context -> {
                    throw new IllegalStateException("转换失败");
                }
        ));
        when(runRepository.save(any(AgentRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRepository.save(any(AgentStepEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(toolCallRepository.save(any(AgentToolCallEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(guardrailResultRepository.save(any(AgentGuardrailResultEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(guardrailService.checkBeforeToolCall(any(), any(), any(), any()))
                .thenReturn(passResults());

        AgentOrchestrator orchestrator = new AgentOrchestrator(
                runRepository,
                toolCallRepository,
                stepRepository,
                guardrailResultRepository,
                toolRegistry,
                guardrailService,
                eventFactory
        );

        orchestrator.convertNovel("nv-1234abcd", ScreenplayTypeEnum.ANIME);

        ArgumentCaptor<AgentRunEntity> runCaptor = ArgumentCaptor.forClass(AgentRunEntity.class);
        verify(runRepository, times(2)).save(runCaptor.capture());
        AgentRunEntity failedRun = runCaptor.getAllValues().get(1);
        assertThat(failedRun.getStatus()).isEqualTo(AgentRunStatusEnum.FAILED);
        assertThat(failedRun.getErrorMessage()).contains("转换失败");

        ArgumentCaptor<AgentStepEntity> stepCaptor = ArgumentCaptor.forClass(AgentStepEntity.class);
        verify(stepRepository, times(2)).save(stepCaptor.capture());
        assertThat(stepCaptor.getAllValues().get(1).getStatus()).isEqualTo(AgentStepStatusEnum.FAILED);
        assertThat(stepCaptor.getAllValues().get(1).getErrorMessage()).contains("转换失败");
    }

    private List<AgentGuardrailCheckResult> passResults() {
        return List.of(
                new AgentGuardrailCheckResult(
                        "tool_registered",
                        AgentGuardrailStatusEnum.PASS,
                        "Tool is registered",
                        "{}"
                ),
                new AgentGuardrailCheckResult(
                        "agent_context_valid",
                        AgentGuardrailStatusEnum.PASS,
                        "Agent context is valid",
                        "{}"
                ),
                new AgentGuardrailCheckResult(
                        "tool_side_effect_allowed",
                        AgentGuardrailStatusEnum.PASS,
                        "Tool side effect WRITE_COST is allowed",
                        "{}"
                )
        );
    }
}
