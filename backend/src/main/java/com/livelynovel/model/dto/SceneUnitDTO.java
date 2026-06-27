package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 章节内场景单元 DTO。
 * 这是章内切场的中间产物，不是最终剧本场景 DTO。
 */
@Getter
@Setter
public class SceneUnitDTO {

    /**
     * 章内场景序号，按 1-based 计数。
     */
    private int sceneIndexInChapter;

    /**
     * 来源章节序号，按 1-based 计数。
     */
    private int sourceChapter;
    private String title;
    private String summary;
    private int startSegmentIndex;
    private int endSegmentIndex;
}
