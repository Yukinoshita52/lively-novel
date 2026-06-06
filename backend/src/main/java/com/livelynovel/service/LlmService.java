package com.livelynovel.service;

import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.dto.SceneUnitDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;

import java.util.List;

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

    /**
     * 章节切场：将单章正文切分为多个场景单元。
     *
     * @param chapterText    单章正文
     * @param chapterIndex   章节序号
     * @param screenplayType 剧本类型（MVP 仅支持 ANIME）
     * @return 章节内场景单元列表
     */
    default List<SceneUnitDTO> splitChapterIntoScenes(
        String chapterText,
        int chapterIndex,
        ScreenplayTypeEnum screenplayType
    ) {
        throw new UnsupportedOperationException("splitChapterIntoScenes not implemented");
    }
}
