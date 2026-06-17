package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 带预览文本的章节轻量 DTO。
 */
@Getter
@Setter
public class ChapterPreviewDTO {

    private int chapterIndex;
    private String title;
    private int wordCount;
    private String preview;
}
