package com.livelynovel.model.dto;

/**
 * 历史小说列表中的单项摘要。
 */
public class NovelListItemDTO {

    private String novelId;
    private String title;
    private int totalChapters;
    private int totalWordCount;
    private String createdAt;
    private String latestConversionId;
    private String latestConversionType;
    private String latestConversionStatus;
    private String latestConversionUpdatedAt;
    private String latestConversionErrorMessage;

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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getLatestConversionId() {
        return latestConversionId;
    }

    public void setLatestConversionId(String latestConversionId) {
        this.latestConversionId = latestConversionId;
    }

    public String getLatestConversionType() {
        return latestConversionType;
    }

    public void setLatestConversionType(String latestConversionType) {
        this.latestConversionType = latestConversionType;
    }

    public String getLatestConversionStatus() {
        return latestConversionStatus;
    }

    public void setLatestConversionStatus(String latestConversionStatus) {
        this.latestConversionStatus = latestConversionStatus;
    }

    public String getLatestConversionUpdatedAt() {
        return latestConversionUpdatedAt;
    }

    public void setLatestConversionUpdatedAt(String latestConversionUpdatedAt) {
        this.latestConversionUpdatedAt = latestConversionUpdatedAt;
    }

    public String getLatestConversionErrorMessage() {
        return latestConversionErrorMessage;
    }

    public void setLatestConversionErrorMessage(String latestConversionErrorMessage) {
        this.latestConversionErrorMessage = latestConversionErrorMessage;
    }
}
