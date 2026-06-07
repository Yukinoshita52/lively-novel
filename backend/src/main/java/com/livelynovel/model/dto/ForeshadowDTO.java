package com.livelynovel.model.dto;

/**
 * 内部伏笔记录，用于后续场景维持线索状态。
 */
public class ForeshadowDTO {
    private String scene;
    private String clue;
    private String status;
    private String note;

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getClue() {
        return clue;
    }

    public void setClue(String clue) {
        this.clue = clue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
