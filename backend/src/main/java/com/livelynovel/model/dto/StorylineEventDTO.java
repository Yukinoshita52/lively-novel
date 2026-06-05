package com.livelynovel.model.dto;

/**
 * 故事线事件 DTO。
 * 对应 yaml-schema.md §5.4 storylines.events 字段。
 */
public class StorylineEventDTO {
    private String scene;
    private String event;

    public StorylineEventDTO() {}

    public StorylineEventDTO(String scene, String event) {
        this.scene = scene;
        this.event = event;
    }

    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
}
