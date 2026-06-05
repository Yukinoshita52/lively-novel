package com.livelynovel.service;

import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * LLM 服务层。
 * 封装 Spring AI ChatClient 调用，提供结构化输出能力。
 */
@Service
public class LlmService {

    private final ChatClient chatClient;

    public LlmService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 单场转换：将小说文本片段转换为结构化场景。
     *
     * @param text           待转换的原文片段
     * @param screenplayType 剧本类型（MVP 仅支持 ANIME）
     * @return 结构化的场景 DTO
     */
    public SceneDTO convertSingleScene(String text, ScreenplayTypeEnum screenplayType) {
        String prompt = buildPrompt(text, screenplayType);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(SceneDTO.class);
    }

    /**
     * 构建 Prompt。
     * MVP 阶段仅实现 ANIME 类型模板。
     */
    private String buildPrompt(String text, ScreenplayTypeEnum screenplayType) {
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
                2. 动作行：描述【可见】的动作与视觉细节，适合动画表现。
                3. 对白：角色名 + 台词；角色名使用原文中的称呼。
                4. 内心戏视觉化：把心理描写/独白转为——
                   - 画外音(V.O.)
                   - 可见动作（表情/肢体）
                   - 镜头特写（眼神/手部等细节）
                   - 说出口的对白
                5. "讲述"变"呈现"：如"她很绝望"→ 转为垂下眼帘、紧握双拳等可画面表现的动作。
                6. 转场：在场景末尾标注切至/淡出等。

                ## 关于 visualizedInnerThoughts 字段
                它是转换留痕：记录"哪句内心描写、用了什么手法、转成了什么"。
                其转换结果应当已经体现在动作行/对白里，不要另起炉灶重复叙述。

                ## 输出要求
                sceneId 使用 "s1" 格式。
                sourceChapter 设为 1。
                sourceText 填入原文。
                """.formatted(text);
    }
}
