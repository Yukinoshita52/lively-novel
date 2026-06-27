package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 章节 DTO。
 * 用于章节分割结果。
 */
@Getter
@Setter
public class ChapterDTO {

    /**
     * 章节序号（从 1 开始）。
     */
    private int chapterIndex;

    /**
     * 章节标题。
     */
    private String title;

    /**
     * 章节正文。
     */
    private String content;

    /**
     * 字数。
     */
    private int wordCount;

    public ChapterDTO() {}

    public ChapterDTO(int chapterIndex, String title, String content, int wordCount) {
        this.chapterIndex = chapterIndex;
        this.title = title;
        this.content = content;
        this.wordCount = wordCount;
    }
}
