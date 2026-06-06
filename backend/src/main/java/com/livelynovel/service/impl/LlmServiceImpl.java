package com.livelynovel.service.impl;

import com.livelynovel.model.dto.ChapterSegmentDTO;
import com.livelynovel.model.dto.DialogueBlockDTO;
import com.livelynovel.model.dto.LlmSceneOutputDTO;
import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.dto.ScriptBlockDTO;
import com.livelynovel.model.dto.SceneUnitDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.service.ChapterSegmentationService;
import com.livelynovel.service.LlmService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 服务实现类。
 * 封装 Spring AI ChatClient 调用，提供结构化输出能力。
 */
@Service
public class LlmServiceImpl implements LlmService {

    private final ChatClient chatClient;
    private final ChapterSegmentationService chapterSegmentationService;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern SCENE_OBJECT_PATTERN = Pattern.compile("\\{[^{}]*\"sceneIndexInChapter\"[^{}]*}", Pattern.DOTALL);
    private static final Pattern JAPANESE_KANA_PATTERN = Pattern.compile("[\\u3040-\\u30ff]");

    public LlmServiceImpl(
            ChatClient.Builder chatClientBuilder,
            ChapterSegmentationService chapterSegmentationService
    ) {
        this.chatClient = chatClientBuilder.build();
        this.chapterSegmentationService = chapterSegmentationService;
    }

    @Override
    public SceneDTO convertSingleScene(String text, ScreenplayTypeEnum screenplayType) {
        String prompt = buildPrompt(normalizeSceneText(text), screenplayType);

        LlmSceneOutputDTO llmScene = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(LlmSceneOutputDTO.class);

        SceneDTO scene = toSceneDTO(llmScene);
        SceneDTO resolvedScene = fillSceneSourceMetadata(scene, text, 1);
        validateSceneLanguage(resolvedScene);
        return resolvedScene;
    }

    @Override
    public List<SceneUnitDTO> splitChapterIntoScenes(
            String chapterText,
            int chapterIndex,
            ScreenplayTypeEnum screenplayType
    ) {
        List<ChapterSegmentDTO> segments = chapterSegmentationService.segment(chapterText);
        String prompt = buildSplitChapterPrompt(segments, chapterIndex, screenplayType);

        String responseText = chatClient.prompt()
                .user(prompt)
                .tools(new ChapterSegmentTools(segments))
                .call()
                .content();

        return parseSplitChapterScenesResult(responseText);
    }

    /**
     * 构建 Prompt。
     * MVP 阶段仅实现 ANIME 类型模板。
     */
    String buildPrompt(String text, ScreenplayTypeEnum screenplayType) {
        // MVP 仅支持 ANIME，后续扩展其他类型
        if (screenplayType != ScreenplayTypeEnum.ANIME) {
            throw new UnsupportedOperationException(
                    "MVP 阶段仅支持 ANIME 类型，暂不支持: " + screenplayType);
        }

        return """
                你是一位资深动画编剧，正在把小说片段改编为【动画剧本】。请将以下原文转换为一个标准动画场景。

                ## 待转换原文
                %s

                ## 转换规则
                1. 场景标题：内景/外景 - 地点 - 时间（如：内景 - 出租屋 - 夜）
                2. 剧本正文必须写入 scriptBlocks 数组，并按实际阅读/演出顺序排列。
                3. scriptBlocks 支持三类：
                   - ACTION：使用 text 描述【可见】的动作与视觉细节。
                   - DIALOGUE：使用 character、parenthetical、line 表达对白；parenthetical 可省略。
                   - TRANSITION：使用 text 表达切至/淡出等转场。
                4. 内心戏视觉化：把心理描写/独白转为——
                   - 画外音(V.O.)
                   - 可见动作（表情/肢体）
                   - 镜头特写（眼神/手部等细节）
                   - 说出口的对白
                   转换后的内容必须落入 scriptBlocks。
                5. "讲述"变"呈现"：如"她很绝望"→ 转为垂下眼帘、紧握双拳等可画面表现的动作。
                6. 转场：在场景末尾以 TRANSITION 块标注切至/淡出等。

                ## 输出要求
                sceneId 使用 "s1" 格式。
                只输出 sceneId、heading、scriptBlocks。
                输出 JSON 结构必须类似：
                {
                  "sceneId": "s1",
                  "heading": {
                    "interior": true,
                    "location": "出租屋",
                    "timeOfDay": "夜"
                  },
                  "scriptBlocks": [
                    { "type": "ACTION", "text": "林晚把简历放在桌上，窗外雨声压过房间里的沉默。" },
                    { "type": "DIALOGUE", "character": "林晚", "parenthetical": "画外音", "line": "我还不能停在这里。" },
                    { "type": "TRANSITION", "text": "切至：便利店门口" }
                  ]
                }
                只输出结构化场景 JSON，不附加解释、前后缀或 Markdown 代码块。
                """.formatted(text);
    }

    static String llmSceneOutputFormat() {
        return new BeanOutputConverter<>(LlmSceneOutputDTO.class).getFormat();
    }

    static SceneDTO toSceneDTO(LlmSceneOutputDTO llmScene) {
        SceneDTO scene = new SceneDTO();
        if (llmScene == null) {
            return scene;
        }

        scene.setSceneId(llmScene.getSceneId());
        scene.setHeading(llmScene.getHeading());
        scene.setScriptBlocks(llmScene.getScriptBlocks());
        return scene;
    }

    static String normalizeSceneText(String text) {
        if (text == null) {
            return "";
        }

        StringBuilder normalized = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '\t') {
                normalized.append("    ");
                continue;
            }
            if (Character.isISOControl(current) && current != '\n' && current != '\r') {
                normalized.append(' ');
                continue;
            }
            normalized.append(current);
        }
        return normalized.toString();
    }

    static SceneDTO fillSceneSourceMetadata(SceneDTO scene, String sourceText, int sourceChapter) {
        SceneDTO resolvedScene = scene == null ? new SceneDTO() : scene;
        resolvedScene.setSourceText(sourceText);
        resolvedScene.setSourceChapter(sourceChapter);
        return resolvedScene;
    }

    // 验证是否语言飘逸（此处策略是检测到有日文）
    static void validateSceneLanguage(SceneDTO scene) {
        if (scene == null) {
            return;
        }

        List<String> generatedTexts = new ArrayList<>();
        if (scene.getHeading() != null) {
            generatedTexts.add(scene.getHeading().getLocation());
            generatedTexts.add(scene.getHeading().getTimeOfDay());
        }
        if (scene.getScriptBlocks() != null) {
            for (ScriptBlockDTO block : scene.getScriptBlocks()) {
                if (block == null) {
                    continue;
                }
                generatedTexts.add(block.getText());
                generatedTexts.add(block.getCharacter());
                generatedTexts.add(block.getParenthetical());
                generatedTexts.add(block.getLine());
            }
        }
        addAll(generatedTexts, scene.getActionLines());
        if (scene.getDialogueBlocks() != null) {
            for (DialogueBlockDTO dialogueBlock : scene.getDialogueBlocks()) {
                if (dialogueBlock == null) {
                    continue;
                }
                generatedTexts.add(dialogueBlock.getCharacter());
                generatedTexts.add(dialogueBlock.getParenthetical());
                generatedTexts.add(dialogueBlock.getLine());
            }
        }
        addAll(generatedTexts, scene.getTransitions());

        for (String text : generatedTexts) {
            if (containsJapaneseKana(text)) {
                throw new IllegalStateException("单场剧本生成出现语言漂移");
            }
        }
    }

    private static void addAll(List<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        target.addAll(values);
    }

    private static boolean containsJapaneseKana(String text) {
        return text != null && JAPANESE_KANA_PATTERN.matcher(text).find();
    }

    static List<SceneUnitDTO> parseSplitChapterScenesResult(String responseText) {
        String json = extractJsonObject(responseText);
        try {
            SplitChapterScenesResult result = OBJECT_MAPPER.readValue(json, SplitChapterScenesResult.class);
            if (result == null || result.getScenes() == null) {
                return List.of();
            }
            return result.getScenes();
        } catch (JsonProcessingException e) {
            List<SceneUnitDTO> fallbackScenes = parseSceneUnitsLeniently(json);
            if (!fallbackScenes.isEmpty()) {
                return fallbackScenes;
            }
            throw new RuntimeException("章节切场结构化解析失败", e);
        }
    }

    static List<SceneUnitDTO> parseSceneUnitsLeniently(String jsonLikeText) {
        if (jsonLikeText == null || jsonLikeText.isBlank()) {
            return List.of();
        }

        List<SceneUnitDTO> scenes = new ArrayList<>();
        Matcher matcher = SCENE_OBJECT_PATTERN.matcher(jsonLikeText);
        while (matcher.find()) {
            String sceneObject = matcher.group();
            Integer sceneIndexInChapter = extractIntField(sceneObject, "sceneIndexInChapter");
            Integer sourceChapter = extractIntField(sceneObject, "sourceChapter");
            Integer startSegmentIndex = extractIntField(sceneObject, "startSegmentIndex");
            Integer endSegmentIndex = extractIntField(sceneObject, "endSegmentIndex");
            if (sceneIndexInChapter == null || startSegmentIndex == null || endSegmentIndex == null) {
                continue;
            }

            SceneUnitDTO scene = new SceneUnitDTO();
            scene.setSceneIndexInChapter(sceneIndexInChapter);
            scene.setSourceChapter(sourceChapter == null ? 1 : sourceChapter);
            scene.setTitle(defaultIfBlank(extractStringField(sceneObject, "title"), "第 " + sceneIndexInChapter + " 场"));
            scene.setSummary(defaultIfBlank(extractStringField(sceneObject, "summary"), ""));
            scene.setStartSegmentIndex(startSegmentIndex);
            scene.setEndSegmentIndex(endSegmentIndex);
            scenes.add(scene);
        }
        return scenes;
    }

    private static Integer extractIntField(String text, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static String extractStringField(String text, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"\\r\\n]*)\"");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    static String extractJsonObject(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            throw new IllegalStateException("章节切场响应为空");
        }

        int fenceStart = responseText.indexOf("```json");
        if (fenceStart >= 0) {
            int jsonStart = responseText.indexOf('{', fenceStart);
            int fenceEnd = responseText.indexOf("```", jsonStart);
            if (jsonStart >= 0 && fenceEnd > jsonStart) {
                return responseText.substring(jsonStart, fenceEnd).strip();
            }
        }

        int firstBrace = responseText.indexOf('{');
        int lastBrace = responseText.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return responseText.substring(firstBrace, lastBrace + 1).strip();
        }

        throw new IllegalStateException("章节切场响应中未找到 JSON 对象");
    }

    /**
     * 构建章节切场 Prompt。
     * MVP 阶段仅实现 ANIME 类型模板。
     */
    String buildSplitChapterPrompt(
            List<ChapterSegmentDTO> segments,
            int chapterIndex,
            ScreenplayTypeEnum screenplayType
    ) {
        if (screenplayType != ScreenplayTypeEnum.ANIME) {
            throw new UnsupportedOperationException(
                    "MVP 阶段仅支持 ANIME 类型，暂不支持: " + screenplayType);
        }

        return """
                你是一位动画编剧策划，正在为小说改编做【章节内场景切分】。
                你的任务不是生成最终剧本，而是把单章正文切成多个连续的场景单元。

                ## 输入信息
                - sourceChapter: %d
                - screenplayType: %s
                - totalSegments: %d

                ## 可用工具
                1. listChapterSegments：查看本章全部编号片段，返回格式为 [segmentIndex] 片段正文
                2. getSegmentRange：根据 startSegmentIndex 和 endSegmentIndex 查看一段连续原文

                ## 切分规则
                1. 依据地点变化、时间跳跃、叙事焦点明显切换来拆分场景。
                2. 同一时间同一地点的连续动作，尽量保持在同一场景单元中。
                3. 请优先调用工具查看片段与区间，不要凭空猜测片段边界。
                4. 不要生成最终剧本字段，不要编写对白格式，不要补写原文没有的新情节。
                5. summary 只写该场景单元发生了什么，保持简洁。
                6. title 只写场景单元的简短标签，便于后续识别。
                7. 每个场景必须返回 startSegmentIndex 和 endSegmentIndex，使用片段编号而不是原文锚点。
                8. title 和 summary 必须使用简体中文，不要使用日文假名、英文夹写或解释性文本。
                9. 不要把多片段长章节整章合并为 1 个场景；除非整章确实只有一个连续场景，否则应按地点、时间或叙事焦点拆成多场。

                ## 输出要求
                1. 返回 scenes 数组。
                2. scenes 中每项包含：
                   - sceneIndexInChapter: 从 1 开始连续编号
                   - sourceChapter: 固定为输入的 sourceChapter
                   - title
                   - summary
                   - startSegmentIndex
                   - endSegmentIndex
                3. 至少输出 1 个场景单元。
                4. 不要输出 startAnchor 或 endAnchor。
                5. 场景区间必须连续覆盖 1 到 totalSegments，不得重叠、跳段或漏段。
                """.formatted(chapterIndex, screenplayType, segments.size());
    }

    static class ChapterSegmentTools {

        private final List<ChapterSegmentDTO> segments;

        ChapterSegmentTools(List<ChapterSegmentDTO> segments) {
            this.segments = segments == null ? List.of() : segments;
        }

        @Tool(description = "列出当前章节的全部编号片段，返回格式为 [segmentIndex] 片段正文")
        String listChapterSegments() {
            StringBuilder builder = new StringBuilder();
            for (ChapterSegmentDTO segment : segments) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append('[')
                        .append(segment.getSegmentIndex())
                        .append("] ")
                        .append(segment.getText());
            }
            return builder.toString();
        }

        @Tool(description = "根据起止片段编号返回连续原文，startSegmentIndex 和 endSegmentIndex 均为闭区间")
        String getSegmentRange(int startSegmentIndex, int endSegmentIndex) {
            if (startSegmentIndex <= 0 || endSegmentIndex < startSegmentIndex || endSegmentIndex > segments.size()) {
                throw new IllegalArgumentException("片段区间非法");
            }

            StringBuilder builder = new StringBuilder();
            for (int index = startSegmentIndex - 1; index < endSegmentIndex; index++) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(segments.get(index).getText());
            }
            return builder.toString();
        }
    }

    /**
     * 章节切场结构化输出包装类型。
     */
    public static class SplitChapterScenesResult {

        private List<SceneUnitDTO> scenes;

        public List<SceneUnitDTO> getScenes() {
            return scenes;
        }

        public void setScenes(List<SceneUnitDTO> scenes) {
            this.scenes = scenes;
        }
    }
}
