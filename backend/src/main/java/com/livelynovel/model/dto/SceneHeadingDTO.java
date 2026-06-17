package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 场景标题 DTO。
 * 对应 yaml-schema.md §5.3 scenes.heading 字段。
 */
@Getter
@Setter
public class SceneHeadingDTO {
    private boolean interior;
    private String location;
    private String timeOfDay;

    public SceneHeadingDTO() {}

    public SceneHeadingDTO(boolean interior, String location, String timeOfDay) {
        this.interior = interior;
        this.location = location;
        this.timeOfDay = timeOfDay;
    }
}
