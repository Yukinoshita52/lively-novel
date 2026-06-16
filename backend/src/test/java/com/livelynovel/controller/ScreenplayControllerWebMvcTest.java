package com.livelynovel.controller;

import com.livelynovel.controller.ScreenplayController;
import com.livelynovel.service.ScreenplayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScreenplayController.class)
class ScreenplayControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScreenplayService screenplayService;

    @Test
    void returnsTextEventStreamForNovelConvert() throws Exception {
        when(screenplayService.convertNovel("nv-1234abcd", com.livelynovel.model.enums.ScreenplayTypeEnum.ANIME))
                .thenAnswer(invocation -> startedEmitter());

        MvcResult result = mockMvc.perform(post("/api/screenplay/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "novelId": "nv-1234abcd",
                                  "screenplayType": "ANIME"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult streamResult = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(streamResult.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
        assertThat(streamResult.getResponse().getContentAsString()).contains("event:started");
        assertThat(streamResult.getResponse().getContentAsString()).contains("event:chapter_split");
        assertThat(streamResult.getResponse().getContentAsString()).contains("event:scene_completed");
    }

    private SseEmitter startedEmitter() {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("started").data(Map.of("novelId", "nv-1234abcd")));
                emitter.send(SseEmitter.event().name("chapter_split")
                        .data(Map.of("chapterIndex", 1, "sceneCount", 2)));
                emitter.send(SseEmitter.event().name("scene_completed")
                        .data(Map.of(
                                "chapterIndex", 1,
                                "sceneIndexInChapter", 1,
                                "title", "站台雨夜",
                                "sceneCount", 2,
                                "scene", Map.of("sceneId", "scene-1")
                        )));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
