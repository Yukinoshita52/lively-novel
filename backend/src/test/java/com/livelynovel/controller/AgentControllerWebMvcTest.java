package com.livelynovel.controller;

import com.livelynovel.agent.AgentOrchestrator;
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

@WebMvcTest(AgentController.class)
class AgentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentOrchestrator orchestrator;

    @Test
    void returnsTextEventStreamForAgentConvert() throws Exception {
        when(orchestrator.convertNovel("nv-1234abcd", com.livelynovel.model.enums.ScreenplayTypeEnum.ANIME))
                .thenReturn(startedEmitter());

        MvcResult result = mockMvc.perform(post("/api/agent/screenplay/convert")
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
        assertThat(streamResult.getResponse().getContentAsString()).contains("event:agent_started");
        assertThat(streamResult.getResponse().getContentAsString()).contains("event:plan_created");
        assertThat(streamResult.getResponse().getContentAsString()).contains("event:step_started");
        assertThat(streamResult.getResponse().getContentAsString()).contains("event:guardrail_checked");
    }

    private SseEmitter startedEmitter() {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("agent_started").data(Map.of("runId", "ar-1234abcd")));
                emitter.send(SseEmitter.event().name("plan_created").data(Map.of("runId", "ar-1234abcd")));
                emitter.send(SseEmitter.event().name("step_started").data(Map.of(
                        "runId", "ar-1234abcd",
                        "stepId", "step-1234abcd"
                )));
                emitter.send(SseEmitter.event().name("guardrail_checked").data(Map.of(
                        "runId", "ar-1234abcd",
                        "stepId", "step-1234abcd",
                        "guardrailName", "tool_side_effect_allowed",
                        "guardrailStatus", "PASS"
                )));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
