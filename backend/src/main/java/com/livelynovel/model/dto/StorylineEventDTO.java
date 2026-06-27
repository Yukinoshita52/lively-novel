package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 故事线事件 DTO。
 * 对应 yaml-schema.md §5.4 storylines.events 字段。
 */
@Getter
@Setter
public class StorylineEventDTO {
    private String scene;
    private String event;

    public StorylineEventDTO() {}

    public StorylineEventDTO(String scene, String event) {
        this.scene = scene;
        this.event = event;
    }
}
