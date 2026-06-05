package com.livelynovel.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局分析结果 DTO。
 * 用于 LLM 阶段A 输出，存入 ScreenplayContentDTO。
 */
public class AnalysisResultDTO {
    private String plotSummary;
    private List<CharacterDTO> characters = new ArrayList<>();
    private List<SceneDTO> scenes = new ArrayList<>();
    private List<StorylineDTO> storylines = new ArrayList<>();

    public AnalysisResultDTO() {}

    public String getPlotSummary() { return plotSummary; }
    public void setPlotSummary(String plotSummary) { this.plotSummary = plotSummary; }
    public List<CharacterDTO> getCharacters() { return characters; }
    public void setCharacters(List<CharacterDTO> characters) { this.characters = characters; }
    public List<SceneDTO> getScenes() { return scenes; }
    public void setScenes(List<SceneDTO> scenes) { this.scenes = scenes; }
    public List<StorylineDTO> getStorylines() { return storylines; }
    public void setStorylines(List<StorylineDTO> storylines) { this.storylines = storylines; }
}
