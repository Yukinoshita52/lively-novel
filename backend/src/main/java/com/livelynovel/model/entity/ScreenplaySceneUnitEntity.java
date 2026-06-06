package com.livelynovel.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 章节切场阶段产物。
 */
@Entity
@Table(
        name = "screenplay_scene_unit",
        uniqueConstraints = @UniqueConstraint(columnNames = {"conversionId", "chapterIndex", "sceneIndexInChapter"})
)
public class ScreenplaySceneUnitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String conversionId;

    @Column(nullable = false)
    private int chapterIndex;

    @Column(nullable = false)
    private int sceneIndexInChapter;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private int startSegmentIndex;

    @Column(nullable = false)
    private int endSegmentIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceText;

    @Column(nullable = false, length = 32)
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConversionId() {
        return conversionId;
    }

    public void setConversionId(String conversionId) {
        this.conversionId = conversionId;
    }

    public int getChapterIndex() {
        return chapterIndex;
    }

    public void setChapterIndex(int chapterIndex) {
        this.chapterIndex = chapterIndex;
    }

    public int getSceneIndexInChapter() {
        return sceneIndexInChapter;
    }

    public void setSceneIndexInChapter(int sceneIndexInChapter) {
        this.sceneIndexInChapter = sceneIndexInChapter;
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

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
