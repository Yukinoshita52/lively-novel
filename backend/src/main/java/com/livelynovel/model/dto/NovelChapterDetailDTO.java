package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 单章正文详情响应体。
 */
@Getter
@Setter
public class NovelChapterDetailDTO {

    private String novelId;
    private int chapterIndex;
    private String title;
    private String content;
    private int wordCount;
}
