package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 内部时间线事件，用于区分叙述顺序与故事内时间。
 */
@Getter
@Setter
public class TimelineEventDTO {
    private String scene;
    private int narrativeOrder;
    private String storyTimeLabel;
    private String event;
    private String certainty;
}
