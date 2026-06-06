package com.livelynovel.model.dto;

/**
 * 已持久化的单场剧本视图。
 */
public class ScreenplayPersistedSceneDTO {

    private int chapterIndex;
    private int sceneIndexInChapter;
    private SceneDTO scene;

    public int getChapterIndex() {
        return chapterIndex;
    }

    public void setChapterIndex(int chapterIndex) {
        this.chapterIndex = chapterIndex;
    }

    public int getSceneIndexInChapter() {
        return sceneIndexInChapter;
    }

    public void setSceneIndexInChapter(int sceneIndexInChapter) {
        this.sceneIndexInChapter = sceneIndexInChapter;
    }

    public SceneDTO getScene() {
        return scene;
    }

    public void setScene(SceneDTO scene) {
        this.scene = scene;
    }
}
