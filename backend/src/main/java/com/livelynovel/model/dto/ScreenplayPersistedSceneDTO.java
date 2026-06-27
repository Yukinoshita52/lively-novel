package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 已持久化的单场剧本视图。
 */
@Getter
@Setter
public class ScreenplayPersistedSceneDTO {

    private int chapterIndex;
    private int sceneIndexInChapter;
    private String title;
    private SceneDTO scene;
}
