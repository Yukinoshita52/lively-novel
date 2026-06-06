package com.livelynovel.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 剧本正文块 DTO。
 * 对应 yaml-schema.md §5.3 scenes.scriptBlocks 字段。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScriptBlockDTO {
    private String type;
    private String text;
    private String character;
    private String parenthetical;
    private String line;

    public ScriptBlockDTO() {}

    public static ScriptBlockDTO action(String text) {
        ScriptBlockDTO block = new ScriptBlockDTO();
        block.setType("ACTION");
        block.setText(text);
        return block;
    }

    public static ScriptBlockDTO dialogue(String character, String parenthetical, String line) {
        ScriptBlockDTO block = new ScriptBlockDTO();
        block.setType("DIALOGUE");
        block.setCharacter(character);
        block.setParenthetical(parenthetical);
        block.setLine(line);
        return block;
    }

    public static ScriptBlockDTO transition(String text) {
        ScriptBlockDTO block = new ScriptBlockDTO();
        block.setType("TRANSITION");
        block.setText(text);
        return block;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getCharacter() { return character; }
    public void setCharacter(String character) { this.character = character; }
    public String getParenthetical() { return parenthetical; }
    public void setParenthetical(String parenthetical) { this.parenthetical = parenthetical; }
    public String getLine() { return line; }
    public void setLine(String line) { this.line = line; }
}
