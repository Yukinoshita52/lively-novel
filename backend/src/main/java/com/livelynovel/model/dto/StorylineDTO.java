package com.livelynovel.model.dto;

import com.livelynovel.model.enums.StorylineTypeEnum;
import java.util.ArrayList;
import java.util.List;

/**
 * 故事线 DTO。
 * 对应 yaml-schema.md §5.4 storylines 字段。
 */
public class StorylineDTO {
    private String name;
    private StorylineTypeEnum type;
    private List<StorylineEventDTO> events = new ArrayList<>();

    public StorylineDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public StorylineTypeEnum getType() { return type; }
    public void setType(StorylineTypeEnum type) { this.type = type; }
    public List<StorylineEventDTO> getEvents() { return events; }
    public void setEvents(List<StorylineEventDTO> events) { this.events = events; }
}
