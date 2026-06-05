package com.livelynovel.service;

import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;

/**
 * LLM 服务接口。
 * 封装 Spring AI ChatClient 调用，提供结构化输出能力。
 */
public interface LlmService {

    /**
     * 单场转换：将小说文本片段转换为结构化场景。
     *
     * @param text           待转换的原文片段
     * @param screenplayType 剧本类型（MVP 仅支持 ANIME）
     * @return 结构化的场景 DTO
     */
    SceneDTO convertSingleScene(String text, ScreenplayTypeEnum screenplayType);
}
