package com.livelynovel.model.entity;

import com.livelynovel.model.enums.ScreenplayTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 整本剧本转换任务。
 */
@Entity
@Table(name = "screenplay_conversion")
public class ScreenplayConversionEntity {

    @Id
    @Column(length = 32, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 32)
    private String novelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ScreenplayTypeEnum screenplayType;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String analysisStateJson;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNovelId() {
        return novelId;
    }

    public void setNovelId(String novelId) {
        this.novelId = novelId;
    }

    public ScreenplayTypeEnum getScreenplayType() {
        return screenplayType;
    }

    public void setScreenplayType(ScreenplayTypeEnum screenplayType) {
        this.screenplayType = screenplayType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getAnalysisStateJson() {
        return analysisStateJson;
    }

    public void setAnalysisStateJson(String analysisStateJson) {
        this.analysisStateJson = analysisStateJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
