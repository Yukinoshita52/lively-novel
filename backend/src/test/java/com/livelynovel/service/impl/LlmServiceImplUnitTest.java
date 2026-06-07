package com.livelynovel.service.impl;

import com.livelynovel.model.dto.ChapterSegmentDTO;
import com.livelynovel.model.dto.ForeshadowDTO;
import com.livelynovel.model.dto.RollingAnalysisStateDTO;
import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.dto.SceneHeadingDTO;
import com.livelynovel.model.dto.ScriptBlockDTO;
import com.livelynovel.model.dto.SceneUnitDTO;
import com.livelynovel.model.dto.StorylineDTO;
import com.livelynovel.model.dto.StorylineEventDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.model.enums.StorylineTypeEnum;
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
    void buildRollingAnalysisPromptTracksTimelineWithoutAssumingNarrativeOrder() {
        LlmServiceImpl service = new LlmServiceImpl(mockBuilder(), chapterText -> List.of());

        String prompt = service.buildRollingAnalysisPrompt(
                new RollingAnalysisStateDTO(),
                sceneUnit(1, 2),
                scene("s2"),
                ScreenplayTypeEnum.ANIME
        );

        assertThat(prompt).contains("滚动全局状态");
        assertThat(prompt).contains("plotSummary");
        assertThat(prompt).contains("characters");
        assertThat(prompt).contains("storylines");
        assertThat(prompt).contains("timeline");
        assertThat(prompt).contains("foreshadows");
        assertThat(prompt).contains("不要因为叙述顺序靠后，就判断事件一定发生得更晚");
        assertThat(prompt).contains("storyTimeLabel");
        assertThat(prompt).contains("certainty");
    }

    @Test
    void buildRollingAnalysisPromptUsesCompactContextAndSummaryRules() {
        LlmServiceImpl service = new LlmServiceImpl(mockBuilder(), chapterText -> List.of());
        RollingAnalysisStateDTO state = new RollingAnalysisStateDTO();
        state.setPlotSummary("旧摘要不应逐字塞进 prompt 造成上下文膨胀。");
        state.setContextSummary("温水已经被卷入八奈见失恋后的关系变化。");
        state.setStorylines(List.of(
                storyline("过早低价值事件", "s1", "很久以前的低价值事件不应进入 prompt。"),
                storyline("近期关系变化", "s11", "八奈见欠下温水餐费，两人关系产生新连接。")
        ));
        ForeshadowDTO foreshadow = new ForeshadowDTO();
        foreshadow.setScene("s3");
        foreshadow.setClue("反复出现的薯条和账单");
        foreshadow.setStatus("OPEN");
        foreshadow.setNote("可能继续制造温水与八奈见的接触。");
        state.setForeshadows(List.of(foreshadow));

        String prompt = service.buildRollingAnalysisPrompt(
                state,
                sceneUnit(2, 1),
                scene("s12"),
                ScreenplayTypeEnum.ANIME
        );

        assertThat(prompt).contains("温水已经被卷入八奈见失恋后的关系变化。");
        assertThat(prompt).contains("近期关系变化");
        assertThat(prompt).contains("八奈见欠下温水餐费，两人关系产生新连接。");
        assertThat(prompt).contains("反复出现的薯条和账单");
        assertThat(prompt).doesNotContain("很久以前的低价值事件不应进入 prompt。");
        assertThat(prompt).contains("plotSummary 不要逐场拼接");
        assertThat(prompt).contains("越久远的事件越概括");
        assertThat(prompt).contains("近期未解决事件要更具体");
        assertThat(prompt).contains("不得编造确定剧情");
        assertThat(prompt).contains("contextSummary");
        assertThat(prompt).contains("activeThreads");
        assertThat(prompt).contains("motifs");
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

    @Test
    void parseRollingAnalysisStateFailureIncludesResponsePreview() {
        String responseText = """
                模型输出了说明文字，但 JSON 不完整。
                {
                  "plotSummary": "温水继续旁观八奈见",
                  "characters": [
                """;

        assertThatThrownBy(() -> LlmServiceImpl.parseRollingAnalysisStateResult(responseText))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("滚动全局状态结构化解析失败")
                .hasMessageContaining("responsePreview=模型输出了说明文字");
    }

    private ChapterSegmentDTO segment(int segmentIndex, String text) {
        ChapterSegmentDTO segment = new ChapterSegmentDTO();
        segment.setSegmentIndex(segmentIndex);
        segment.setText(text);
        return segment;
    }

    private SceneUnitDTO sceneUnit(int chapterIndex, int sceneIndexInChapter) {
        SceneUnitDTO sceneUnit = new SceneUnitDTO();
        sceneUnit.setSourceChapter(chapterIndex);
        sceneUnit.setSceneIndexInChapter(sceneIndexInChapter);
        sceneUnit.setTitle("家庭餐厅的偶遇");
        sceneUnit.setSummary("温水听见八奈见和袴田争执。");
        sceneUnit.setStartSegmentIndex(1);
        sceneUnit.setEndSegmentIndex(3);
        return sceneUnit;
    }

    private SceneDTO scene(String sceneId) {
        SceneDTO scene = new SceneDTO();
        scene.setSceneId(sceneId);
        scene.setHeading(new SceneHeadingDTO(true, "家庭餐厅", "午后"));
        scene.setScriptBlocks(List.of(ScriptBlockDTO.action("温水抬头看向隔壁桌。")));
        scene.setSourceChapter(1);
        return scene;
    }

    private StorylineDTO storyline(String name, String sceneId, String eventText) {
        StorylineDTO storyline = new StorylineDTO();
        storyline.setName(name);
        storyline.setType(StorylineTypeEnum.MAIN);
        storyline.setEvents(List.of(new StorylineEventDTO(sceneId, eventText)));
        return storyline;
    }

    private ChatClient.Builder mockBuilder() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);
        when(builder.build()).thenReturn(client);
        return builder;
    }
}
