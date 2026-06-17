package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 对白块 DTO。
 * 对应 yaml-schema.md §5.3 scenes.dialogueBlocks 字段。
 */
@Getter
@Setter
public class DialogueBlockDTO {
    private String character;
    private String parenthetical;
    private String line;

    public DialogueBlockDTO() {}

    public DialogueBlockDTO(String character, String parenthetical, String line) {
        this.character = character;
        this.parenthetical = parenthetical;
        this.line = line;
    }
}
