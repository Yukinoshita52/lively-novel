package com.livelynovel.model.dto;

import com.livelynovel.model.enums.VisualizationMethodEnum;

import lombok.Getter;
import lombok.Setter;

/**
 * 内心戏视觉化 DTO。
 * 对应 yaml-schema.md §5.3 scenes.visualizedInnerThoughts 字段。
 */
@Getter
@Setter
public class VisualizedInnerThoughtDTO {
    private String original;
    private VisualizationMethodEnum method;
    private String result;

    public VisualizedInnerThoughtDTO() {}

    public VisualizedInnerThoughtDTO(String original, VisualizationMethodEnum method, String result) {
        this.original = original;
        this.method = method;
        this.result = result;
    }
}
