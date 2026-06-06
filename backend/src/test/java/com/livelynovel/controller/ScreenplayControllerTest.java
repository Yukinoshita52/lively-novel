package com.livelynovel.controller;

import com.livelynovel.controller.ScreenplayController;
import com.livelynovel.model.dto.ScreenplayConvertRequestDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.service.LlmService;
import com.livelynovel.service.ScreenplayService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScreenplayControllerTest {

    private final LlmService llmService = mock(LlmService.class);
    private final ScreenplayService screenplayService = mock(ScreenplayService.class);
    private final ScreenplayController controller = new ScreenplayController(llmService, screenplayService);

    @Test
    void rejectsEmptyNovelIdBeforeStartingConvertStream() {
        ScreenplayConvertRequestDTO request = new ScreenplayConvertRequestDTO();
        request.setNovelId(" ");

        Object response = controller.convert(request);

        assertThat(response).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> entity = (ResponseEntity<?>) response;
        assertThat(entity.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void startsConvertStreamForStoredNovel() {
        ScreenplayConvertRequestDTO request = new ScreenplayConvertRequestDTO();
        request.setNovelId("nv-1234abcd");
        request.setScreenplayType(ScreenplayTypeEnum.ANIME);

        when(screenplayService.convertNovel("nv-1234abcd", ScreenplayTypeEnum.ANIME))
                .thenReturn(new SseEmitter(0L));

        Object response = controller.convert(request);

        assertThat(response).isInstanceOf(SseEmitter.class);
    }
}
