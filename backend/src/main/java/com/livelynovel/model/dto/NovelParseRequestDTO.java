package com.livelynovel.model.dto;

/**
 * 小说解析请求体。
 * 对应 {@code POST /api/novel/parse}（详见 technical-design.md §4.4 接口④）。
 */
public class NovelParseRequestDTO {

    /** 小说标题（可选）。 */
    private String title;

    /** 原始小说正文。 */
    private String text;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
