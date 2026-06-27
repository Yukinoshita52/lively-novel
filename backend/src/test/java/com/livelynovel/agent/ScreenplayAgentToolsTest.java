package com.livelynovel.agent;

import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.service.ScreenplayService;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScreenplayAgentToolsTest {

    @Test
    void registersExistingConversionTool() {
        ToolRegistry registry = new ToolRegistry();
        ScreenplayService screenplayService = mock(ScreenplayService.class);
        when(screenplayService.convertNovel("nv-1234abcd", ScreenplayTypeEnum.ANIME))
                .thenReturn(new SseEmitter(0L));
        when(screenplayService.getLatestConversionId("nv-1234abcd", ScreenplayTypeEnum.ANIME))
                .thenReturn("cv-1234abcd");

        new ScreenplayAgentTools(registry, screenplayService);

        AgentToolResult result = registry.execute("runExistingScreenplayConversion", new AgentToolContext(
                "ar-1234abcd",
                "nv-1234abcd",
                ScreenplayTypeEnum.ANIME,
                null
        ));

        verify(screenplayService).convertNovel("nv-1234abcd", ScreenplayTypeEnum.ANIME);
        assertThat(result.conversionId()).isEqualTo("cv-1234abcd");
        assertThat(result.outputJson()).contains("cv-1234abcd");
    }
}
