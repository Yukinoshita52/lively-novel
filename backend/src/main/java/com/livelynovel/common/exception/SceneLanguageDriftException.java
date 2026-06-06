package com.livelynovel.common.exception;

import com.livelynovel.model.dto.SceneDTO;

/**
 * 单场剧本生成语言漂移异常。
 * 记录命中字段和短文本，方便定位是 LLM 输出问题还是校验误判。
 */
public class SceneLanguageDriftException extends IllegalStateException {
    private final String fieldPath;
    private final String textPreview;
    private final SceneDTO scene;

    public SceneLanguageDriftException(String fieldPath, String textPreview) {
        this(fieldPath, textPreview, null);
    }

    public SceneLanguageDriftException(String fieldPath, String textPreview, SceneDTO scene) {
        super("单场剧本生成出现语言漂移：fieldPath=" + fieldPath + ", textPreview=" + textPreview);
        this.fieldPath = fieldPath;
        this.textPreview = textPreview;
        this.scene = scene;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public String getTextPreview() {
        return textPreview;
    }

    public SceneDTO getScene() {
        return scene;
    }
}
