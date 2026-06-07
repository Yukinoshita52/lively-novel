package com.livelynovel.model.dto;

/**
 * 内部时间线事件，用于区分叙述顺序与故事内时间。
 */
public class TimelineEventDTO {
    private String scene;
    private int narrativeOrder;
    private String storyTimeLabel;
    private String event;
    private String certainty;

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public int getNarrativeOrder() {
        return narrativeOrder;
    }

    public void setNarrativeOrder(int narrativeOrder) {
        this.narrativeOrder = narrativeOrder;
    }

    public String getStoryTimeLabel() {
        return storyTimeLabel;
    }

    public void setStoryTimeLabel(String storyTimeLabel) {
        this.storyTimeLabel = storyTimeLabel;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getCertainty() {
        return certainty;
    }

    public void setCertainty(String certainty) {
        this.certainty = certainty;
    }
}
