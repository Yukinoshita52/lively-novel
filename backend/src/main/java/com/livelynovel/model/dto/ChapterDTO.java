package com.livelynovel.model.dto;

/**
 * 章节 DTO。
 * 用于章节分割结果。
 */
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

    public int getChapterIndex() {
        return chapterIndex;
    }

    public void setChapterIndex(int chapterIndex) {
        this.chapterIndex = chapterIndex;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }
}
