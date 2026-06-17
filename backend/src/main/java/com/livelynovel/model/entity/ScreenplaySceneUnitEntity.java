package com.livelynovel.model.entity;

import jakarta.persistence.Column;

import lombok.Getter;
import lombok.Setter;
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
@Getter
@Setter
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
}
