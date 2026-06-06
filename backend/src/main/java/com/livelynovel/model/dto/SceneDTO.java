package com.livelynovel.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 场景 DTO。
 * 对应 yaml-schema.md §5.3 scenes 字段。
 */
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

    public String getSceneId() { return sceneId; }
    public void setSceneId(String sceneId) { this.sceneId = sceneId; }
    public SceneHeadingDTO getHeading() { return heading; }
    public void setHeading(SceneHeadingDTO heading) { this.heading = heading; }
    public List<ScriptBlockDTO> getScriptBlocks() { return scriptBlocks; }
    public void setScriptBlocks(List<ScriptBlockDTO> scriptBlocks) { this.scriptBlocks = scriptBlocks; }
    public List<String> getActionLines() { return actionLines; }
    public void setActionLines(List<String> actionLines) { this.actionLines = actionLines; }
    public List<DialogueBlockDTO> getDialogueBlocks() { return dialogueBlocks; }
    public void setDialogueBlocks(List<DialogueBlockDTO> dialogueBlocks) { this.dialogueBlocks = dialogueBlocks; }
    public List<VisualizedInnerThoughtDTO> getVisualizedInnerThoughts() { return visualizedInnerThoughts; }
    public void setVisualizedInnerThoughts(List<VisualizedInnerThoughtDTO> visualizedInnerThoughts) { this.visualizedInnerThoughts = visualizedInnerThoughts; }
    public List<String> getTransitions() { return transitions; }
    public void setTransitions(List<String> transitions) { this.transitions = transitions; }
    public int getSourceChapter() { return sourceChapter; }
    public void setSourceChapter(int sourceChapter) { this.sourceChapter = sourceChapter; }
    public String getSourceText() { return sourceText; }
    public void setSourceText(String sourceText) { this.sourceText = sourceText; }
}
