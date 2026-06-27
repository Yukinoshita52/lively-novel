package com.livelynovel.model.entity;

import com.livelynovel.model.enums.AgentRunStatusEnum;
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

import lombok.Getter;
import lombok.Setter;

/**
 * Agent run trace root record.
 */
@Entity
@Table(name = "agent_run")
@Getter
@Setter
public class AgentRunEntity {

    @Id
    @Column(length = 32, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userGoal;

    @Column(nullable = false, length = 32)
    private String novelId;

    @Column(length = 32)
    private String conversionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ScreenplayTypeEnum screenplayType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AgentRunStatusEnum status;

    @Column(nullable = false)
    private int currentStepIndex;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant completedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String finalArtifactRef;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (startedAt == null) {
            startedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
