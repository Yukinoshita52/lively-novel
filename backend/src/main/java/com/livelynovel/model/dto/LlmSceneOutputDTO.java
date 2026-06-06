package com.livelynovel.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 单场生成输出 DTO。
 * 仅用于结构化输出约束，不包含内部溯源字段或旧版剧本字段。
 */
public class LlmSceneOutputDTO {
    private String sceneId;
    private SceneHeadingDTO heading;
    private List<ScriptBlockDTO> scriptBlocks = new ArrayList<>();

    public LlmSceneOutputDTO() {}

    public String getSceneId() { return sceneId; }
    public void setSceneId(String sceneId) { this.sceneId = sceneId; }
    public SceneHeadingDTO getHeading() { return heading; }
    public void setHeading(SceneHeadingDTO heading) { this.heading = heading; }
    public List<ScriptBlockDTO> getScriptBlocks() { return scriptBlocks; }
    public void setScriptBlocks(List<ScriptBlockDTO> scriptBlocks) { this.scriptBlocks = scriptBlocks; }
}
