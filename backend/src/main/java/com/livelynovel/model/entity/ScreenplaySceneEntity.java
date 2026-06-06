package com.livelynovel.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 单场剧本生成阶段产物。
 */
@Entity
@Table(
        name = "screenplay_scene",
        uniqueConstraints = @UniqueConstraint(columnNames = {"conversionId", "chapterIndex", "sceneIndexInChapter"})
)
public class ScreenplaySceneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String conversionId;

    @Column(nullable = false)
    private int chapterIndex;

    @Column(nullable = false)
    private int sceneIndexInChapter;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sceneJson;

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

    public String getSceneJson() {
        return sceneJson;
    }

    public void setSceneJson(String sceneJson) {
        this.sceneJson = sceneJson;
    }
}
