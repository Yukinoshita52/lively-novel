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
 * 单场剧本生成阶段产物。
 */
@Entity
@Table(
        name = "screenplay_scene",
        uniqueConstraints = @UniqueConstraint(columnNames = {"conversionId", "chapterIndex", "sceneIndexInChapter"})
)
@Getter
@Setter
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
}
