package com.livelynovel.model.dto;

import java.util.List;

/**
 * 小说解析响应体。
 * 对应 {@code POST /api/novel/parse} 的 {@code data}（详见 technical-design.md §4.4 接口④）。
 *
 * <p>本切片为无状态解析，不落库，故不含 {@code novelId} / {@code contentHash}。
 */
public class NovelParseResultDTO {

    /** 小说标题。 */
    private String title;

    /** 章节总数。 */
    private int totalChapters;

    /** 总字数（各章字数之和）。 */
    private int totalWordCount;

    /** 章节列表（仅含标题与字数等元信息，正文不回传）。 */
    private List<ChapterDTO> chapters;

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

    public List<ChapterDTO> getChapters() {
        return chapters;
    }

    public void setChapters(List<ChapterDTO> chapters) {
        this.chapters = chapters;
    }
}
