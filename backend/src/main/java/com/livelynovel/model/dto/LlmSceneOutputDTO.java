package com.livelynovel.model.dto;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * LLM 单场生成输出 DTO。
 * 仅用于结构化输出约束，不包含内部溯源字段或旧版剧本字段。
 */
@Getter
@Setter
public class LlmSceneOutputDTO {
    private String sceneId;
    private SceneHeadingDTO heading;
    private List<ScriptBlockDTO> scriptBlocks = new ArrayList<>();

    public LlmSceneOutputDTO() {}
}
