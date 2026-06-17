package com.livelynovel.model.dto;

import com.livelynovel.model.enums.ScreenplayTypeEnum;

import lombok.Getter;
import lombok.Setter;

/**
 * 整本转换请求 DTO。
 */
@Getter
@Setter
public class ScreenplayConvertRequestDTO {

    private String novelId;
    private ScreenplayTypeEnum screenplayType = ScreenplayTypeEnum.ANIME;
}
