package com.livelynovel.service.impl;

import com.livelynovel.model.dto.ChapterSegmentDTO;
import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.dto.SceneHeadingDTO;
import com.livelynovel.model.dto.ScriptBlockDTO;
import com.livelynovel.model.dto.SceneUnitDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.common.exception.SceneLanguageDriftException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmServiceImplUnitTest {

    @Test
    void buildPromptDoesNotAskModelToReturnSourceTextOrSourceChapter() {
        LlmServiceImpl service = new LlmServiceImpl(mockBuilder(), chapterText -> List.of());

        String prompt = service.buildPrompt("片段原文", ScreenplayTypeEnum.ANIME);

        assertThat(prompt).contains("scriptBlocks");
        assertThat(prompt).contains("\"sceneId\": \"s1\"");
        assertThat(prompt).contains("\"type\": \"ACTION\"");
        assertThat(prompt).contains("\"type\": \"SHOT\"");
        assertThat(prompt).contains("\"type\": \"INSERT\"");
        assertThat(prompt).contains("\"type\": \"SFX\"");
        assertThat(prompt).contains("\"type\": \"DIALOGUE\"");
        assertThat(prompt).contains("\"type\": \"VO\"");
        assertThat(prompt).contains("\"type\": \"TRANSITION\"");
        assertThat(prompt).contains("ACTION 只写一个可拍摄动作节拍");
        assertThat(prompt).contains("不要把环境、道具、镜头、音效、内心独白硬塞进 ACTION");
        assertThat(prompt).doesNotContain("actionLines");
        assertThat(prompt).doesNotContain("dialogueBlocks");
        assertThat(prompt).doesNotContain("visualizedInnerThoughts");
        assertThat(prompt).doesNotContain("sourceText");
        assertThat(prompt).doesNotContain("sourceChapter");
    }

    @Test
    void llmSceneOutputFormatOnlyExposesNativeScriptBlocksFields() {
        String format = LlmServiceImpl.llmSceneOutputFormat();

        assertThat(format).contains("sceneId");
        assertThat(format).contains("heading");
        assertThat(format).contains("scriptBlocks");
        assertThat(format).contains("character");
        assertThat(format).contains("parenthetical");
        assertThat(format).contains("line");
        assertThat(format).doesNotContain("actionLines");
        assertThat(format).doesNotContain("dialogueBlocks");
        assertThat(format).doesNotContain("transitions");
        assertThat(format).doesNotContain("visualizedInnerThoughts");
        assertThat(format).doesNotContain("sourceChapter");
        assertThat(format).doesNotContain("sourceText");
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
    void rejectSceneWhenGeneratedTextContainsJapaneseKana() {
        SceneDTO scene = new SceneDTO();
        scene.setHeading(new SceneHeadingDTO(true, "教室", "ほうかご"));
        scene.setScriptBlocks(List.of(
                ScriptBlockDTO.action("林秋把书包放在桌上。"),
                ScriptBlockDTO.dialogue("林秋", null, "もう大丈夫。")
        ));

        assertThatThrownBy(() -> LlmServiceImpl.validateSceneLanguage(scene))
                .isInstanceOf(SceneLanguageDriftException.class)
                .hasMessageContaining("语言漂移")
                .extracting(error -> ((SceneLanguageDriftException) error).getFieldPath())
                .isEqualTo("heading.timeOfDay");
    }

    @Test
    void languageDriftExceptionIncludesMatchedFieldAndPreview() {
        SceneDTO scene = new SceneDTO();
        scene.setHeading(new SceneHeadingDTO(true, "教室", "放学后"));
        scene.setScriptBlocks(List.of(
                ScriptBlockDTO.action("林秋把书包放在桌上。"),
                ScriptBlockDTO.dialogue("林秋", null, "もう大丈夫。")
        ));

        assertThatThrownBy(() -> LlmServiceImpl.validateSceneLanguage(scene))
                .isInstanceOf(SceneLanguageDriftException.class)
                .extracting(error -> {
                    SceneLanguageDriftException drift = (SceneLanguageDriftException) error;
                    return List.of(drift.getFieldPath(), drift.getTextPreview());
                })
                .isEqualTo(List.of("scriptBlocks[1].line", "もう大丈夫。"));
    }

    @Test
    void acceptsSceneWhenGeneratedTextIsChinese() {
        SceneDTO scene = new SceneDTO();
        scene.setHeading(new SceneHeadingDTO(true, "教室", "放学后"));
        scene.setScriptBlocks(List.of(
                ScriptBlockDTO.action("林秋把书包放在桌上。"),
                ScriptBlockDTO.textBlock("SHOT", "走廊尽头。夕阳把地面拉成长条。"),
                ScriptBlockDTO.textBlock("INSERT", "桌面上的自行车钥匙。"),
                ScriptBlockDTO.textBlock("SFX", "门铃急促响起。"),
                ScriptBlockDTO.voiceOver("林秋", "画外音", "我已经不能停在这里。"),
                ScriptBlockDTO.dialogue("林秋", null, "我已经没事了。"),
                ScriptBlockDTO.transition("切至：走廊")
        ));
        scene.setSourceText("原文里即使出现かな也不参与生成文本语言检测。");

        LlmServiceImpl.validateSceneLanguage(scene);
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
        assertThat(prompt).contains("必须使用简体中文");
        assertThat(prompt).contains("不要把多片段长章节整章合并为 1 个场景");
        assertThat(prompt).contains("连续覆盖 1 到 totalSegments");
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

    @Test
    void parseSingleSceneResultRepairsUnescapedQuotesInScriptBlockText() {
        String responseText = """
                {
                  "sceneId": "s1",
                  "heading": {
                    "interior": true,
                    "location": "家庭餐厅",
                    "timeOfDay": "午后"
                  },
                  "scriptBlocks": [
                    {
                      "type": "SFX",
                      "text": "服务员的声音："久等了，这是大份薯条！""
                    }
                  ]
                }
                """;

        SceneDTO scene = LlmServiceImpl.parseSingleSceneResult(responseText);

        assertThat(scene.getSceneId()).isEqualTo("s1");
        assertThat(scene.getScriptBlocks()).hasSize(1);
        assertThat(scene.getScriptBlocks().get(0).getType()).isEqualTo("SFX");
        assertThat(scene.getScriptBlocks().get(0).getText()).isEqualTo("服务员的声音：\"久等了，这是大份薯条！\"");
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
