package com.livelynovel.model.dto;

/**
 * 场景标题 DTO。
 * 对应 yaml-schema.md §5.3 scenes.heading 字段。
 */
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

    public boolean isInterior() { return interior; }
    public void setInterior(boolean interior) { this.interior = interior; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }
}
