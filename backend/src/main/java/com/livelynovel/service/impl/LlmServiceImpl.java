package com.livelynovel.service.impl;

import com.livelynovel.model.dto.ChapterSegmentDTO;
import com.livelynovel.model.dto.DialogueBlockDTO;
import com.livelynovel.model.dto.LlmSceneOutputDTO;
import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.dto.ScriptBlockDTO;
import com.livelynovel.model.dto.SceneUnitDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.common.exception.SceneLanguageDriftException;
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

        String responseText = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        SceneDTO scene = parseSingleSceneResult(responseText);
        SceneDTO resolvedScene = fillSceneSourceMetadata(scene, text, 1);
        try {
            validateSceneLanguage(resolvedScene);
        } catch (SceneLanguageDriftException e) {
            throw new SceneLanguageDriftException(e.getFieldPath(), e.getTextPreview(), resolvedScene);
        }
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
                3. scriptBlocks 支持七类：
                   - SHOT：使用 text 描述镜头/构图/视线焦点，例如"家庭餐厅一角。午后的光从窗边斜照进来。"
                   - ACTION：使用 text 描述【一个】可拍摄动作节拍；ACTION 只写一个可拍摄动作节拍。
                   - INSERT：使用 text 描述插入特写，例如书封、钥匙、账单、玻璃杯。
                   - SFX：使用 text 描述音效，例如门铃、咳嗽、脚步声。
                   - DIALOGUE：使用 character、parenthetical、line 表达对白；parenthetical 可省略。
                   - VO：使用 character、parenthetical、line 表达画外音/内心独白；parenthetical 通常为"画外音"。
                   - TRANSITION：使用 text 表达切至/淡出等转场。
                4. 内心戏视觉化：把心理描写/独白转为——
                   - VO 画外音
                   - 可见动作（表情/肢体）
                   - SHOT 或 INSERT 镜头特写（眼神/手部/道具等细节）
                   - 说出口的对白
                   转换后的内容必须落入 scriptBlocks。
                5. "讲述"变"呈现"：如"她很绝望"→ 转为垂下眼帘、紧握双拳等可画面表现的动作。
                6. 不要把环境、道具、镜头、音效、内心独白硬塞进 ACTION；分别使用 SHOT、INSERT、SFX、VO。
                7. 转场：在场景末尾以 TRANSITION 块标注切至/淡出等。
                8. 所有场景标题、动作、对白、括号提示、转场正文都使用简体中文表达。

                ## few-shot 示例
                {
                  "sceneId": "s1",
                  "heading": {
                    "interior": true,
                    "location": "家庭餐厅",
                    "timeOfDay": "午后"
                  },
                  "scriptBlocks": [
                    { "type": "SHOT", "text": "家庭餐厅一角。午后的光从窗边斜照进来，店内客人稀疏。" },
                    { "type": "ACTION", "text": "温水坐在靠墙的位置，用手帕擦去额头的汗。" },
                    { "type": "ACTION", "text": "他警惕地环视四周，确认附近没有同校学生。" },
                    { "type": "INSERT", "text": "桌面上摆着自助饮料杯、大份薯条，以及一本轻小说最新卷。" },
                    { "type": "SFX", "text": "隔壁桌传来女声尖叫。" },
                    { "type": "VO", "character": "温水", "parenthetical": "画外音", "line": "我也想尝试这样的恋爱。" },
                    { "type": "DIALOGUE", "character": "八奈见", "line": "这样不行啦草介！" },
                    { "type": "TRANSITION", "text": "切至：收银台前" }
                  ]
                }
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

    static SceneDTO parseSingleSceneResult(String responseText) {
        String json = extractJsonObject(responseText);
        try {
            return toSceneDTO(OBJECT_MAPPER.readValue(json, LlmSceneOutputDTO.class));
        } catch (JsonProcessingException e) {
            try {
                return toSceneDTO(OBJECT_MAPPER.readValue(repairUnescapedQuotesInJsonStrings(json), LlmSceneOutputDTO.class));
            } catch (JsonProcessingException repairedError) {
                throw new RuntimeException("单场剧本结构化解析失败", repairedError);
            }
        }
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

        List<GeneratedTextField> generatedTexts = new ArrayList<>();
        if (scene.getHeading() != null) {
            generatedTexts.add(new GeneratedTextField("heading.location", scene.getHeading().getLocation()));
            generatedTexts.add(new GeneratedTextField("heading.timeOfDay", scene.getHeading().getTimeOfDay()));
        }
        if (scene.getScriptBlocks() != null) {
            for (int i = 0; i < scene.getScriptBlocks().size(); i++) {
                ScriptBlockDTO block = scene.getScriptBlocks().get(i);
                if (block == null) {
                    continue;
                }
                generatedTexts.add(new GeneratedTextField("scriptBlocks[" + i + "].text", block.getText()));
                generatedTexts.add(new GeneratedTextField("scriptBlocks[" + i + "].character", block.getCharacter()));
                generatedTexts.add(new GeneratedTextField("scriptBlocks[" + i + "].parenthetical", block.getParenthetical()));
                generatedTexts.add(new GeneratedTextField("scriptBlocks[" + i + "].line", block.getLine()));
            }
        }
        addAll(generatedTexts, "actionLines", scene.getActionLines());
        if (scene.getDialogueBlocks() != null) {
            for (int i = 0; i < scene.getDialogueBlocks().size(); i++) {
                DialogueBlockDTO dialogueBlock = scene.getDialogueBlocks().get(i);
                if (dialogueBlock == null) {
                    continue;
                }
                generatedTexts.add(new GeneratedTextField("dialogueBlocks[" + i + "].character", dialogueBlock.getCharacter()));
                generatedTexts.add(new GeneratedTextField("dialogueBlocks[" + i + "].parenthetical", dialogueBlock.getParenthetical()));
                generatedTexts.add(new GeneratedTextField("dialogueBlocks[" + i + "].line", dialogueBlock.getLine()));
            }
        }
        addAll(generatedTexts, "transitions", scene.getTransitions());

        for (GeneratedTextField field : generatedTexts) {
            if (containsJapaneseKana(field.text())) {
                throw new SceneLanguageDriftException(field.path(), previewText(field.text()));
            }
        }
    }

    private record GeneratedTextField(String path, String text) {}

    private static void addAll(List<GeneratedTextField> target, String fieldName, List<String> values) {
        if (values == null) {
            return;
        }
        for (int i = 0; i < values.size(); i++) {
            target.add(new GeneratedTextField(fieldName + "[" + i + "]", values.get(i)));
        }
    }

    private static boolean containsJapaneseKana(String text) {
        return text != null && JAPANESE_KANA_PATTERN.matcher(text).find();
    }

    private static String previewText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 80) {
            return normalized;
        }
        return normalized.substring(0, 80) + "...";
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

    static String repairUnescapedQuotesInJsonStrings(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }

        StringBuilder repaired = new StringBuilder(json.length());
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char current = json.charAt(i);

            if (escaped) {
                repaired.append(current);
                escaped = false;
                continue;
            }

            if (current == '\\') {
                repaired.append(current);
                escaped = inString;
                continue;
            }

            if (current == '"') {
                if (!inString) {
                    inString = true;
                    repaired.append(current);
                    continue;
                }

                if (isLikelyJsonStringTerminator(json, i + 1)) {
                    inString = false;
                    repaired.append(current);
                } else {
                    repaired.append("\\\"");
                }
                continue;
            }

            repaired.append(current);
        }

        return repaired.toString();
    }

    private static boolean isLikelyJsonStringTerminator(String json, int nextIndex) {
        for (int i = nextIndex; i < json.length(); i++) {
            char next = json.charAt(i);
            if (Character.isWhitespace(next)) {
                continue;
            }
            return next == ',' || next == '}' || next == ']' || next == ':';
        }
        return true;
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
