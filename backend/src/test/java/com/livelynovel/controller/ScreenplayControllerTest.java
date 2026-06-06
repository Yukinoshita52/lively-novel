package com.livelynovel.controller;

import com.livelynovel.controller.ScreenplayController;
import com.livelynovel.common.Result;
import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.dto.ScreenplayConversionDetailDTO;
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

    @Test
    void returnsPersistedConversionDetail() {
        ScreenplayConversionDetailDTO detail = new ScreenplayConversionDetailDTO();
        detail.setConversionId("cv-1234abcd");
        detail.setStatus("COMPLETED");
        when(screenplayService.getConversionDetail("cv-1234abcd")).thenReturn(detail);

        Result<ScreenplayConversionDetailDTO> response = controller.getConversionDetail("cv-1234abcd");

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getData().getConversionId()).isEqualTo("cv-1234abcd");
    }

    @Test
    void returnsNotFoundWhenConversionMissing() {
        when(screenplayService.getConversionDetail("cv-missing")).thenReturn(null);

        Result<ScreenplayConversionDetailDTO> response = controller.getConversionDetail("cv-missing");

        assertThat(response.getCode()).isEqualTo(40401);
        assertThat(response.getData()).isNull();
    }

    @Test
    void returnsLatestCompletedConversionForNovelAndType() {
        ScreenplayConversionDetailDTO detail = new ScreenplayConversionDetailDTO();
        detail.setConversionId("cv-completed");
        detail.setStatus("COMPLETED");
        when(screenplayService.getLatestCompletedConversion("nv-1234abcd", ScreenplayTypeEnum.ANIME))
                .thenReturn(detail);

        Result<ScreenplayConversionDetailDTO> response =
                controller.getLatestCompletedConversion("nv-1234abcd", ScreenplayTypeEnum.ANIME);

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getData().getConversionId()).isEqualTo("cv-completed");
    }

    @Test
    void returnsNotFoundWhenLatestCompletedConversionMissing() {
        when(screenplayService.getLatestCompletedConversion("nv-missing", ScreenplayTypeEnum.ANIME))
                .thenReturn(null);

        Result<ScreenplayConversionDetailDTO> response =
                controller.getLatestCompletedConversion("nv-missing", ScreenplayTypeEnum.ANIME);

        assertThat(response.getCode()).isEqualTo(40401);
        assertThat(response.getData()).isNull();
    }

    @Test
    void exportsConversionYaml() {
        when(screenplayService.exportConversionYaml("cv-1234abcd"))
                .thenReturn("schemaVersion: \"1.0\"\nscenes: []\n");

        ResponseEntity<String> response = controller.exportConversionYaml("cv-1234abcd");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString()).contains("text/yaml");
        assertThat(response.getBody()).contains("schemaVersion");
    }

    @Test
    void returnsNotFoundWhenExportConversionMissing() {
        when(screenplayService.exportConversionYaml("cv-missing")).thenReturn(null);

        ResponseEntity<String> response = controller.exportConversionYaml("cv-missing");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).contains("转换任务不存在");
    }

    @Test
    void updatesPersistedScene() {
        SceneDTO scene = new SceneDTO();
        scene.setSceneId("s1");
        scene.setSourceChapter(1);
        scene.setSourceText("第一段原文。");
        when(screenplayService.updatePersistedScene("cv-1234abcd", 1, 1, scene)).thenReturn(scene);

        Result<SceneDTO> response = controller.updatePersistedScene("cv-1234abcd", 1, 1, scene);

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getData().getSceneId()).isEqualTo("s1");
    }

    @Test
    void returnsNotFoundWhenUpdatingMissingScene() {
        SceneDTO scene = new SceneDTO();
        when(screenplayService.updatePersistedScene("cv-missing", 1, 1, scene)).thenReturn(null);

        Result<SceneDTO> response = controller.updatePersistedScene("cv-missing", 1, 1, scene);

        assertThat(response.getCode()).isEqualTo(40401);
        assertThat(response.getData()).isNull();
    }
}
