package com.livelynovel.model.dto;

import com.livelynovel.model.enums.ScreenplayTypeEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * 整本转换持久化详情。
 */
public class ScreenplayConversionDetailDTO {

    private String conversionId;
    private String novelId;
    private ScreenplayTypeEnum screenplayType;
    private String status;
    private String errorMessage;
    private List<ScreenplayPersistedSceneDTO> scenes = new ArrayList<>();

    public String getConversionId() {
        return conversionId;
    }

    public void setConversionId(String conversionId) {
        this.conversionId = conversionId;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<ScreenplayPersistedSceneDTO> getScenes() {
        return scenes;
    }

    public void setScenes(List<ScreenplayPersistedSceneDTO> scenes) {
        this.scenes = scenes;
    }
}
