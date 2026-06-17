package com.livelynovel.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

/**
 * 剧本正文块 DTO。
 * 对应 yaml-schema.md §5.3 scenes.scriptBlocks 字段。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class ScriptBlockDTO {
    private String type;
    private String text;
    private String character;
    private String parenthetical;
    private String line;

    public ScriptBlockDTO() {}

    public static ScriptBlockDTO action(String text) {
        return textBlock("ACTION", text);
    }

    public static ScriptBlockDTO textBlock(String type, String text) {
        ScriptBlockDTO block = new ScriptBlockDTO();
        block.setType(type);
        block.setText(text);
        return block;
    }

    public static ScriptBlockDTO dialogue(String character, String parenthetical, String line) {
        return spokenBlock("DIALOGUE", character, parenthetical, line);
    }

    public static ScriptBlockDTO voiceOver(String character, String parenthetical, String line) {
        return spokenBlock("VO", character, parenthetical, line);
    }

    public static ScriptBlockDTO spokenBlock(String type, String character, String parenthetical, String line) {
        ScriptBlockDTO block = new ScriptBlockDTO();
        block.setType(type);
        block.setCharacter(character);
        block.setParenthetical(parenthetical);
        block.setLine(line);
        return block;
    }

    public static ScriptBlockDTO transition(String text) {
        return textBlock("TRANSITION", text);
    }
}
