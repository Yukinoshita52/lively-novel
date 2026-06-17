package com.livelynovel.model.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 查询已存小说章节列表的响应体。
 */
@Getter
@Setter
public class NovelChaptersResultDTO {

    private String novelId;
    private String title;
    private int totalChapters;
    private int totalWordCount;
    private List<ChapterPreviewDTO> chapters;
}
