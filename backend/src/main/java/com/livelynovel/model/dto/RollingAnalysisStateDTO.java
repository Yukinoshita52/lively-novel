package com.livelynovel.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * 转换过程中的滚动全局认知状态。
 * 导出 YAML 只使用 plotSummary、characters、storylines；
 * timeline 与 foreshadows 作为内部状态，帮助后续场景避免时序与伏笔误判。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollingAnalysisStateDTO {
    private String contextSummary;
    private String plotSummary;
    private List<ActiveCharacterDTO> activeCharacters = new ArrayList<>();
    private List<CharacterDTO> characters = new ArrayList<>();
    private List<ActiveThreadDTO> activeThreads = new ArrayList<>();
    private List<StorylineDTO> storylines = new ArrayList<>();
    private List<MotifDTO> motifs = new ArrayList<>();
    private List<TimelineEventDTO> timeline = new ArrayList<>();
    private List<ForeshadowDTO> foreshadows = new ArrayList<>();

    public String getContextSummary() {
        return contextSummary;
    }

    public void setContextSummary(String contextSummary) {
        this.contextSummary = contextSummary;
    }

    public String getPlotSummary() {
        return plotSummary;
    }

    public void setPlotSummary(String plotSummary) {
        this.plotSummary = plotSummary;
    }

    public List<ActiveCharacterDTO> getActiveCharacters() {
        return activeCharacters;
    }

    public void setActiveCharacters(List<ActiveCharacterDTO> activeCharacters) {
        this.activeCharacters = activeCharacters;
    }

    public List<CharacterDTO> getCharacters() {
        return characters;
    }

    public void setCharacters(List<CharacterDTO> characters) {
        this.characters = characters;
    }

    public List<ActiveThreadDTO> getActiveThreads() {
        return activeThreads;
    }

    public void setActiveThreads(List<ActiveThreadDTO> activeThreads) {
        this.activeThreads = activeThreads;
    }

    public List<StorylineDTO> getStorylines() {
        return storylines;
    }

    public void setStorylines(List<StorylineDTO> storylines) {
        this.storylines = storylines;
    }

    public List<MotifDTO> getMotifs() {
        return motifs;
    }

    public void setMotifs(List<MotifDTO> motifs) {
        this.motifs = motifs;
    }

    public List<TimelineEventDTO> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<TimelineEventDTO> timeline) {
        this.timeline = timeline;
    }

    public List<ForeshadowDTO> getForeshadows() {
        return foreshadows;
    }

    public void setForeshadows(List<ForeshadowDTO> foreshadows) {
        this.foreshadows = foreshadows;
    }
}
