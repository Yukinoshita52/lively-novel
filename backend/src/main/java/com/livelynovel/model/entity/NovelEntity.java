package com.livelynovel.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * 小说持久化实体。
 */
@Entity
@Table(name = "novel")
@Getter
@Setter
public class NovelEntity {

    @Id
    @Column(length = 32, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 80)
    private String contentHash;

    @Column(nullable = false)
    private int totalChapters;

    @Column(nullable = false)
    private int totalWordCount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawContent;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
