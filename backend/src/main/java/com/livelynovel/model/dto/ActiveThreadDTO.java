package com.livelynovel.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 近期仍影响剧情的事件、矛盾或关系线，用于下一场上下文选择。
 */
public class ActiveThreadDTO {
    private String name;
    private String status;
    private String importance;
    private String recentSummary;
    private List<String> relatedCharacters = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImportance() {
        return importance;
    }

    public void setImportance(String importance) {
        this.importance = importance;
    }

    public String getRecentSummary() {
        return recentSummary;
    }

    public void setRecentSummary(String recentSummary) {
        this.recentSummary = recentSummary;
    }

    public List<String> getRelatedCharacters() {
        return relatedCharacters;
    }

    public void setRelatedCharacters(List<String> relatedCharacters) {
        this.relatedCharacters = relatedCharacters;
    }
}
