package com.livelynovel.model.dto;

import com.livelynovel.model.enums.ScreenplayTypeEnum;

/**
 * 整本转换请求 DTO。
 */
public class ScreenplayConvertRequestDTO {

    private String novelId;
    private ScreenplayTypeEnum screenplayType = ScreenplayTypeEnum.ANIME;

    public String getNovelId() {
        return novelId;
    }

    public void setNovelId(String novelId) {
        this.novelId = novelId;
    }

    public ScreenplayTypeEnum getScreenplayType() {
        return screenplayType;
    }

    public void setScreenplayType(ScreenplayTypeEnum screenplayType) {
        this.screenplayType = screenplayType;
    }
}
