package com.livelynovel.model.dto;

import java.util.List;

/**
 * 查询已存小说章节列表的响应体。
 */
public class NovelChaptersResultDTO {

    private String novelId;
    private String title;
    private int totalChapters;
    private int totalWordCount;
    private List<ChapterPreviewDTO> chapters;

    public String getNovelId() {
        return novelId;
    }

    public void setNovelId(String novelId) {
        this.novelId = novelId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getTotalChapters() {
        return totalChapters;
    }

    public void setTotalChapters(int totalChapters) {
        this.totalChapters = totalChapters;
    }

    public int getTotalWordCount() {
        return totalWordCount;
    }

    public void setTotalWordCount(int totalWordCount) {
        this.totalWordCount = totalWordCount;
    }

    public List<ChapterPreviewDTO> getChapters() {
        return chapters;
    }

    public void setChapters(List<ChapterPreviewDTO> chapters) {
        this.chapters = chapters;
    }
}
