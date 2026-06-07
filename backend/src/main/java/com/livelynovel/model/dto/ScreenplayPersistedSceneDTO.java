package com.livelynovel.model.dto;

/**
 * 已持久化的单场剧本视图。
 */
public class ScreenplayPersistedSceneDTO {

    private int chapterIndex;
    private int sceneIndexInChapter;
    private String title;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public SceneDTO getScene() {
        return scene;
    }

    public void setScene(SceneDTO scene) {
        this.scene = scene;
    }
}
