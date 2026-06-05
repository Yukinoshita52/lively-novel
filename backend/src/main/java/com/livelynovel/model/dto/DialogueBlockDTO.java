package com.livelynovel.model.dto;

/**
 * 对白块 DTO。
 * 对应 yaml-schema.md §5.3 scenes.dialogueBlocks 字段。
 */
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

    public String getCharacter() { return character; }
    public void setCharacter(String character) { this.character = character; }
    public String getParenthetical() { return parenthetical; }
    public void setParenthetical(String parenthetical) { this.parenthetical = parenthetical; }
    public String getLine() { return line; }
    public void setLine(String line) { this.line = line; }
}
