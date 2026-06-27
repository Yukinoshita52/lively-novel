package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 重复出现的意象、物件或符号，用于识别伏笔和关系线，不进入最终 YAML。
 */
@Getter
@Setter
public class MotifDTO {
    private String name;
    private String meaning;
    private String firstScene;
    private String lastScene;
    private int occurrenceCount;
}
