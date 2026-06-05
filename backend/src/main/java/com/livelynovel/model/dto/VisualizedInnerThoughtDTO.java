package com.livelynovel.model.dto;

import com.livelynovel.model.enums.VisualizationMethodEnum;

/**
 * 内心戏视觉化 DTO。
 * 对应 yaml-schema.md §5.3 scenes.visualizedInnerThoughts 字段。
 */
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

    public String getOriginal() { return original; }
    public void setOriginal(String original) { this.original = original; }
    public VisualizationMethodEnum getMethod() { return method; }
    public void setMethod(VisualizationMethodEnum method) { this.method = method; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
