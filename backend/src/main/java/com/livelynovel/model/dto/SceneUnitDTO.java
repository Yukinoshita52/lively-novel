package com.livelynovel.model.dto;

/**
 * 章节内场景单元 DTO。
 * 这是章内切场的中间产物，不是最终剧本场景 DTO。
 */
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

    public int getSceneIndexInChapter() {
        return sceneIndexInChapter;
    }

    public void setSceneIndexInChapter(int sceneIndexInChapter) {
        this.sceneIndexInChapter = sceneIndexInChapter;
    }

    public int getSourceChapter() {
        return sourceChapter;
    }

    public void setSourceChapter(int sourceChapter) {
        this.sourceChapter = sourceChapter;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getStartSegmentIndex() {
        return startSegmentIndex;
    }

    public void setStartSegmentIndex(int startSegmentIndex) {
        this.startSegmentIndex = startSegmentIndex;
    }

    public int getEndSegmentIndex() {
        return endSegmentIndex;
    }

    public void setEndSegmentIndex(int endSegmentIndex) {
        this.endSegmentIndex = endSegmentIndex;
    }
}
