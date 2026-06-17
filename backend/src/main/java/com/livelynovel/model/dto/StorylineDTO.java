package com.livelynovel.model.dto;

import com.livelynovel.model.enums.StorylineTypeEnum;
import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * 故事线 DTO。
 * 对应 yaml-schema.md §5.4 storylines 字段。
 */
@Getter
@Setter
public class StorylineDTO {
    private String name;
    private StorylineTypeEnum type;
    private List<StorylineEventDTO> events = new ArrayList<>();

    public StorylineDTO() {}
}
