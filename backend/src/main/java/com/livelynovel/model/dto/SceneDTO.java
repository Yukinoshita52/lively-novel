package com.livelynovel.model.dto;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * 场景 DTO。
 * 对应 yaml-schema.md §5.3 scenes 字段。
 */
@Getter
@Setter
public class SceneDTO {
    private String sceneId;
    private SceneHeadingDTO heading;
    private List<ScriptBlockDTO> scriptBlocks = new ArrayList<>();
    private List<String> actionLines = new ArrayList<>();
    private List<DialogueBlockDTO> dialogueBlocks = new ArrayList<>();
    private List<VisualizedInnerThoughtDTO> visualizedInnerThoughts = new ArrayList<>();
    private List<String> transitions = new ArrayList<>();
    private int sourceChapter;
    private String sourceText;

    public SceneDTO() {}
}
