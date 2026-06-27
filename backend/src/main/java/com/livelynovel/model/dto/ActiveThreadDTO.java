package com.livelynovel.model.dto;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * 近期仍影响剧情的事件、矛盾或关系线，用于下一场上下文选择。
 */
@Getter
@Setter
public class ActiveThreadDTO {
    private String name;
    private String status;
    private String importance;
    private String recentSummary;
    private List<String> relatedCharacters = new ArrayList<>();
}
