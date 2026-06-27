package com.livelynovel.agent;

import com.livelynovel.model.entity.AgentRunEntity;
import com.livelynovel.model.entity.AgentToolCallEntity;
import com.livelynovel.model.enums.AgentRunStatusEnum;
import com.livelynovel.model.enums.AgentToolSideEffectLevelEnum;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.repository.AgentRunRepository;
import com.livelynovel.repository.AgentToolCallRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentOrchestratorTest {

    private final AgentRunRepository runRepository = mock(AgentRunRepository.class);
    private final AgentToolCallRepository toolCallRepository = mock(AgentToolCallRepository.class);
    private final AgentSseEventFactory eventFactory = new AgentSseEventFactory();

    @Test
    void executesFixedPlanAndPersistsTrace() {
        List<AgentRunStatusEnum> savedRunStatuses = new ArrayList<>();
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
        when(toolCallRepository.save(any(AgentToolCallEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgentOrchestrator orchestrator = new AgentOrchestrator(
                runRepository,
                toolCallRepository,
                toolRegistry,
                eventFactory
        );

        SseEmitter emitter = orchestrator.convertNovel("nv-1234abcd", ScreenplayTypeEnum.ANIME);

        assertThat(emitter).isNotNull();
        ArgumentCaptor<AgentRunEntity> runCaptor = ArgumentCaptor.forClass(AgentRunEntity.class);
        verify(runRepository, times(2)).save(runCaptor.capture());
        assertThat(savedRunStatuses).containsExactly(AgentRunStatusEnum.RUNNING, AgentRunStatusEnum.COMPLETED);
        assertThat(runCaptor.getAllValues().get(1).getStatus()).isEqualTo(AgentRunStatusEnum.COMPLETED);
        assertThat(runCaptor.getAllValues().get(1).getConversionId()).isEqualTo("cv-1234abcd");

        ArgumentCaptor<AgentToolCallEntity> toolCallCaptor = ArgumentCaptor.forClass(AgentToolCallEntity.class);
        verify(toolCallRepository, times(2)).save(toolCallCaptor.capture());
        assertThat(toolCallCaptor.getAllValues().get(0).getToolName())
                .isEqualTo("runExistingScreenplayConversion");
        assertThat(toolCallCaptor.getAllValues().get(1).getOutputJson())
                .contains("cv-1234abcd");
    }

    @Test
    void marksRunFailedWhenToolFails() {
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
        when(toolCallRepository.save(any(AgentToolCallEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgentOrchestrator orchestrator = new AgentOrchestrator(
                runRepository,
                toolCallRepository,
                toolRegistry,
                eventFactory
        );

        orchestrator.convertNovel("nv-1234abcd", ScreenplayTypeEnum.ANIME);

        ArgumentCaptor<AgentRunEntity> runCaptor = ArgumentCaptor.forClass(AgentRunEntity.class);
        verify(runRepository, times(2)).save(runCaptor.capture());
        AgentRunEntity failedRun = runCaptor.getAllValues().get(1);
        assertThat(failedRun.getStatus()).isEqualTo(AgentRunStatusEnum.FAILED);
        assertThat(failedRun.getErrorMessage()).contains("转换失败");
    }
}
