package com.livelynovel.model.dto;

/**
 * 章节编号片段 DTO。
 */
public class ChapterSegmentDTO {

    private int segmentIndex;
    private String text;

    public int getSegmentIndex() {
        return segmentIndex;
    }

    public void setSegmentIndex(int segmentIndex) {
        this.segmentIndex = segmentIndex;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
