package com.livelynovel.controller;

import com.livelynovel.agent.AgentOrchestrator;
import com.livelynovel.model.dto.ScreenplayConvertRequestDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentControllerTest {

    private final AgentOrchestrator orchestrator = mock(AgentOrchestrator.class);
    private final AgentController controller = new AgentController(orchestrator);

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
}
