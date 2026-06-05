package com.livelynovel.service;

import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LlmService 单场转换测试。
 * 验证 LLM 调用和结构化输出功能。
 *
 * 使用方式：
 * 1. 复制 application-local.yml.example 为 application-local.yml
 * 2. 在 application-local.yml 中填入你的 DEEPSEEK_API_KEY
 * 3. 运行测试（会自动激活 local profile）
 */
@SpringBootTest(properties = "spring.profiles.active=local")
class LlmServiceTest {

    @Autowired
    private LlmService llmService;

    /**
     * 测试单场转换功能。
     * 使用 yaml-schema.md §7 示例中的 s1 原文片段。
     */
    @Test
    void testConvertSingleScene() {
        // 示例原文（来自 yaml-schema.md §7 的 s1 sourceText）
        String sourceText = "雨敲在铁皮屋檐上。林晚把简历又看了一遍，心里像压了块石头——她知道这次又没戏了。";

        try {
            // 调用转换
            SceneDTO scene = llmService.convertSingleScene(sourceText, ScreenplayTypeEnum.ANIME);

            // 验证返回结构不为空
            assertNotNull(scene, "场景 DTO 不应为空");

            // 验证基本字段
            assertNotNull(scene.getSceneId(), "sceneId 不应为空");
            assertNotNull(scene.getHeading(), "heading 不应为空");
            assertNotNull(scene.getActionLines(), "actionLines 不应为空");
            assertNotNull(scene.getSourceText(), "sourceText 不应为空");

            // 验证 heading 结构
            assertNotNull(scene.getHeading().getLocation(), "location 不应为空");
            assertNotNull(scene.getHeading().getTimeOfDay(), "timeOfDay 不应为空");

            // 验证溯源字段
            assertEquals(1, scene.getSourceChapter(), "sourceChapter 应为 1");
            assertTrue(scene.getSourceText().contains("林晚"),
                    "sourceText 应包含原文内容");

            // 打印结果便于调试
            System.out.println("=== 转换结果 ===");
            System.out.println("sceneId: " + scene.getSceneId());
            System.out.println("heading: " + (scene.getHeading().isInterior() ? "内景" : "外景")
                    + " - " + scene.getHeading().getLocation()
                    + " - " + scene.getHeading().getTimeOfDay());
            System.out.println("actionLines 数量: " + scene.getActionLines().size());
            for (String action : scene.getActionLines()) {
                System.out.println("  - " + action);
            }
            System.out.println("dialogueBlocks 数量: " + scene.getDialogueBlocks().size());
            for (var dialogue : scene.getDialogueBlocks()) {
                System.out.println("  - " + dialogue.getCharacter() + ": " + dialogue.getLine());
            }
            System.out.println("visualizedInnerThoughts 数量: " + scene.getVisualizedInnerThoughts().size());
            for (var thought : scene.getVisualizedInnerThoughts()) {
                System.out.println("  - [" + thought.getMethod() + "] "
                        + thought.getOriginal() + " → " + thought.getResult());
            }
            System.out.println("transitions: " + scene.getTransitions());

        } catch (Exception e) {
            System.err.println("LLM 调用失败: " + e.getMessage());
            e.printStackTrace();

            // 提供排查建议
            if (e.getMessage().contains("authentication") || e.getMessage().contains("401")) {
                System.err.println("\n排查建议:");
                System.err.println("1. 检查 DEEPSEEK_API_KEY 是否正确设置");
                System.err.println("2. 检查 API Key 是否有效（登录 DeepSeek 控制台确认）");
                System.err.println("3. 检查 API Key 是否有余额");
            }

            fail("LLM 调用失败: " + e.getMessage());
        }
    }

    /**
     * 测试非 ANIME 类型应抛出异常。
     */
    @Test
    void testUnsupportedScreenplayType() {
        String sourceText = "测试文本";

        assertThrows(UnsupportedOperationException.class, () -> {
            llmService.convertSingleScene(sourceText, ScreenplayTypeEnum.FILM);
        }, "MVP 阶段仅支持 ANIME 类型，其他类型应抛出异常");
    }
}