package com.livelynovel.model.dto;

import com.livelynovel.model.enums.ScreenplayTypeEnum;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * 整本转换持久化详情。
 */
@Getter
@Setter
public class ScreenplayConversionDetailDTO {

    private String conversionId;
    private String novelId;
    private ScreenplayTypeEnum screenplayType;
    private String status;
    private String errorMessage;
    private String analysisStateJson;
    private List<ScreenplayPersistedSceneDTO> scenes = new ArrayList<>();
}
