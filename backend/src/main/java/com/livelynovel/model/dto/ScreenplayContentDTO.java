package com.livelynovel.model.dto;

import com.livelynovel.model.enums.ScreenplayTypeEnum;
import java.util.ArrayList;
import java.util.List;

/**
 * 剧本内容 DTO，序列化为 JSON 存入 Screenplay.contentJson。
 * 对应 yaml-schema.md §4 顶层结构。
 */
public class ScreenplayContentDTO {
    private String schemaVersion = "1.0";
    private String title;
    private ScreenplayTypeEnum screenplayType;
    private String plotSummary;
    private List<CharacterDTO> characters = new ArrayList<>();
    private List<SceneDTO> scenes = new ArrayList<>();
    private List<StorylineDTO> storylines = new ArrayList<>();

    public ScreenplayContentDTO() {}

    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public ScreenplayTypeEnum getScreenplayType() { return screenplayType; }
    public void setScreenplayType(ScreenplayTypeEnum screenplayType) { this.screenplayType = screenplayType; }
    public String getPlotSummary() { return plotSummary; }
    public void setPlotSummary(String plotSummary) { this.plotSummary = plotSummary; }
    public List<CharacterDTO> getCharacters() { return characters; }
    public void setCharacters(List<CharacterDTO> characters) { this.characters = characters; }
    public List<SceneDTO> getScenes() { return scenes; }
    public void setScenes(List<SceneDTO> scenes) { this.scenes = scenes; }
    public List<StorylineDTO> getStorylines() { return storylines; }
    public void setStorylines(List<StorylineDTO> storylines) { this.storylines = storylines; }
}
