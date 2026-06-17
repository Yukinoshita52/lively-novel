package com.livelynovel.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * 转换过程中的滚动全局认知状态。
 * 导出 YAML 只使用 plotSummary、characters、storylines；
 * timeline 与 foreshadows 作为内部状态，帮助后续场景避免时序与伏笔误判。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
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
}
