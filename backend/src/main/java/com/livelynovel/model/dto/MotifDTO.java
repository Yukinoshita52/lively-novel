package com.livelynovel.model.dto;

/**
 * 重复出现的意象、物件或符号，用于识别伏笔和关系线，不进入最终 YAML。
 */
public class MotifDTO {
    private String name;
    private String meaning;
    private String firstScene;
    private String lastScene;
    private int occurrenceCount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public String getFirstScene() {
        return firstScene;
    }

    public void setFirstScene(String firstScene) {
        this.firstScene = firstScene;
    }

    public String getLastScene() {
        return lastScene;
    }

    public void setLastScene(String lastScene) {
        this.lastScene = lastScene;
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(int occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }
}
