package com.livelynovel.controller;

import com.livelynovel.agent.AgentOrchestrator;
import com.livelynovel.agent.AgentTraceService;
import com.livelynovel.common.Result;
import com.livelynovel.model.dto.AgentTraceDTO;
import com.livelynovel.model.dto.ScreenplayConvertRequestDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentControllerTest {

    private final AgentOrchestrator orchestrator = mock(AgentOrchestrator.class);
    private final AgentTraceService traceService = mock(AgentTraceService.class);
    private final AgentController controller = new AgentController(orchestrator, traceService);

    @Test
    void rejectsEmptyNovelIdBeforeStartingAgentStream() {
        ScreenplayConvertRequestDTO request = new ScreenplayConvertRequestDTO();
        request.setNovelId(" ");
        request.setScreenplayType(ScreenplayTypeEnum.ANIME);

        Object response = controller.convert(request);

        assertThat(response).isInstanceOf(ResponseEntity.class);
        assertThat(((ResponseEntity<?>) response).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void startsAgentConvertStream() {
        ScreenplayConvertRequestDTO request = new ScreenplayConvertRequestDTO();
        request.setNovelId("nv-1234abcd");
        request.setScreenplayType(ScreenplayTypeEnum.ANIME);
        when(orchestrator.convertNovel("nv-1234abcd", ScreenplayTypeEnum.ANIME))
                .thenReturn(new SseEmitter(0L));

        Object response = controller.convert(request);

        assertThat(response).isInstanceOf(SseEmitter.class);
    }

    @Test
    void returnsTraceForExistingRun() {
        AgentTraceDTO trace = new AgentTraceDTO();
        trace.setRunId("ar-1234abcd");
        when(traceService.getTrace("ar-1234abcd")).thenReturn(Optional.of(trace));

        Result<AgentTraceDTO> response = controller.getTrace("ar-1234abcd");

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getData().getRunId()).isEqualTo("ar-1234abcd");
    }

    @Test
    void returnsBusinessErrorWhenTraceRunMissing() {
        when(traceService.getTrace("missing")).thenReturn(Optional.empty());

        Result<AgentTraceDTO> response = controller.getTrace("missing");

        assertThat(response.getCode()).isEqualTo(40401);
        assertThat(response.getMessage()).isEqualTo("Agent run 不存在");
    }

    @Test
    void rejectsBlankRunIdForTraceRead() {
        Result<AgentTraceDTO> response = controller.getTrace(" ");

        assertThat(response.getCode()).isEqualTo(40001);
        assertThat(response.getMessage()).isEqualTo("runId 不能为空");
    }
}
