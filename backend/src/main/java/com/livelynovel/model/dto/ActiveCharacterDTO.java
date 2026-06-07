package com.livelynovel.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 近期活跃人物状态，用于下一场上下文选择，不进入最终 YAML。
 */
public class ActiveCharacterDTO {
    private String name;
    private String recentState;
    private String longTermRole;
    private String lastSeenScene;
    private List<String> unresolvedEvents = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRecentState() {
        return recentState;
    }

    public void setRecentState(String recentState) {
        this.recentState = recentState;
    }

    public String getLongTermRole() {
        return longTermRole;
    }

    public void setLongTermRole(String longTermRole) {
        this.longTermRole = longTermRole;
    }

    public String getLastSeenScene() {
        return lastSeenScene;
    }

    public void setLastSeenScene(String lastSeenScene) {
        this.lastSeenScene = lastSeenScene;
    }

    public List<String> getUnresolvedEvents() {
        return unresolvedEvents;
    }

    public void setUnresolvedEvents(List<String> unresolvedEvents) {
        this.unresolvedEvents = unresolvedEvents;
    }
}
