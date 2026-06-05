package com.livelynovel.model.dto;

import com.livelynovel.model.enums.ScreenplayTypeEnum;

/**
 * 单场转换请求 DTO。
 * 用于最小转换切片：输入文本 → LLM → 返回 SceneDTO。
 */
public class SingleSceneConvertRequestDTO {

    /**
     * 待转换的原文片段。
     */
    private String text;

    /**
     * 剧本类型，默认 ANIME。
     */
    private ScreenplayTypeEnum screenplayType = ScreenplayTypeEnum.ANIME;

    public SingleSceneConvertRequestDTO() {}

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ScreenplayTypeEnum getScreenplayType() {
        return screenplayType;
    }

    public void setScreenplayType(ScreenplayTypeEnum screenplayType) {
        this.screenplayType = screenplayType;
    }
}
