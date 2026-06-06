package com.livelynovel.service.impl;

import com.livelynovel.model.dto.ChapterSegmentDTO;
import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.dto.SceneUnitDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmServiceImplUnitTest {

    @Test
    void buildPromptDoesNotAskModelToReturnSourceTextOrSourceChapter() {
        LlmServiceImpl service = new LlmServiceImpl(mockBuilder(), chapterText -> List.of());

        String prompt = service.buildPrompt("片段原文", ScreenplayTypeEnum.ANIME);

        assertThat(prompt).doesNotContain("sourceText 填入原文");
        assertThat(prompt).doesNotContain("sourceChapter 设为 1");
        assertThat(prompt).contains("不要输出 sourceText 字段");
        assertThat(prompt).contains("不要输出 sourceChapter 字段");
    }

    @Test
    void normalizeSceneTextReplacesTabAndOtherControlCharacters() {
        String normalized = LlmServiceImpl.normalizeSceneText("第一行\t片段\u0007\n第二行");

        assertThat(normalized).isEqualTo("第一行    片段 \n第二行");
    }

    @Test
    void fillSceneSourceMetadataUsesLocalValues() {
        SceneDTO scene = new SceneDTO();

        SceneDTO filled = LlmServiceImpl.fillSceneSourceMetadata(scene, "片段原文\tA", 3);

        assertThat(filled.getSourceText()).isEqualTo("片段原文\tA");
        assertThat(filled.getSourceChapter()).isEqualTo(3);
    }

    @Test
    void buildSplitChapterPromptUsesSegmentIndexesInsteadOfAnchors() {
        LlmServiceImpl service = new LlmServiceImpl(mockBuilder(), chapterText -> List.of());

        String prompt = service.buildSplitChapterPrompt(List.of(segment(1, "第一段"), segment(2, "第二段")), 1, ScreenplayTypeEnum.ANIME);

        assertThat(prompt).contains("startSegmentIndex");
        assertThat(prompt).contains("endSegmentIndex");
        assertThat(prompt).contains("listChapterSegments");
        assertThat(prompt).contains("getSegmentRange");
        assertThat(prompt).contains("不要输出 startAnchor 或 endAnchor");
    }

    @Test
    void chapterSegmentToolsExposeListAndRangeOperations() {
        LlmServiceImpl.ChapterSegmentTools tools = new LlmServiceImpl.ChapterSegmentTools(List.of(
                segment(1, "第一段"),
                segment(2, "第二段"),
                segment(3, "第三段")
        ));

        String segmentList = tools.listChapterSegments();
        String rangeText = tools.getSegmentRange(2, 3);

        assertThat(segmentList).contains("[1] 第一段");
        assertThat(segmentList).contains("[3] 第三段");
        assertThat(rangeText).isEqualTo("第二段\n第三段");
    }

    @Test
    void parseSplitChapterScenesResultExtractsJsonFromNarrationAndCodeFence() {
        String responseText = """
                Now I have a complete picture of the chapter.

                ```json
                {
                  "scenes": [
                    {
                      "sceneIndexInChapter": 1,
                      "sourceChapter": 1,
                      "title": "家庭餐厅的偶遇",
                      "summary": "温水在家庭餐厅目睹八奈见失恋。",
                      "startSegmentIndex": 1,
                      "endSegmentIndex": 12
                    }
                  ]
                }
                ```
                """;

        List<SceneUnitDTO> scenes = LlmServiceImpl.parseSplitChapterScenesResult(responseText);

        assertThat(scenes).hasSize(1);
        assertThat(scenes.get(0).getSceneIndexInChapter()).isEqualTo(1);
        assertThat(scenes.get(0).getStartSegmentIndex()).isEqualTo(1);
        assertThat(scenes.get(0).getEndSegmentIndex()).isEqualTo(12);
    }

    @Test
    void extractJsonObjectReturnsJsonBodyFromMixedResponse() {
        String responseText = """
                analysis first
                {
                  "scenes": []
                }
                """;

        String json = LlmServiceImpl.extractJsonObject(responseText);

        assertThat(json).contains("\"scenes\": []");
        assertThat(json.strip()).startsWith("{");
        assertThat(json.strip()).endsWith("}");
    }

    @Test
    void parseSplitChapterScenesResultFallsBackWhenStringFieldContainsUnescapedQuotes() {
        String responseText = """
                ```json
                {
                  "scenes": [
                    {
                      "sceneIndexInChapter": 11,
                      "sourceChapter": 1,
                      "title": "周三午餐",
                      "summary": "八奈见谈到自己是"青梅竹马"时情绪崩溃。",
                      "startSegmentIndex": 954,
                      "endSegmentIndex": 1057
                    }
                  ]
                }
                ```
                """;

        List<SceneUnitDTO> scenes = LlmServiceImpl.parseSplitChapterScenesResult(responseText);

        assertThat(scenes).hasSize(1);
        assertThat(scenes.get(0).getSceneIndexInChapter()).isEqualTo(11);
        assertThat(scenes.get(0).getSourceChapter()).isEqualTo(1);
        assertThat(scenes.get(0).getStartSegmentIndex()).isEqualTo(954);
        assertThat(scenes.get(0).getEndSegmentIndex()).isEqualTo(1057);
        assertThat(scenes.get(0).getTitle()).isEqualTo("周三午餐");
    }

    private ChapterSegmentDTO segment(int segmentIndex, String text) {
        ChapterSegmentDTO segment = new ChapterSegmentDTO();
        segment.setSegmentIndex(segmentIndex);
        segment.setText(text);
        return segment;
    }

    private ChatClient.Builder mockBuilder() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);
        when(builder.build()).thenReturn(client);
        return builder;
    }
}
