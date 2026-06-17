package com.livelynovel.model.dto;

import com.livelynovel.model.enums.CharacterRoleEnum;
import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * 人物 DTO。
 * 对应 yaml-schema.md §5.2 characters 字段。
 */
@Getter
@Setter
public class CharacterDTO {
    private String name;
    private CharacterRoleEnum role;
    private String description;
    private String firstAppearance;
    private List<CharacterRelationshipDTO> relationships = new ArrayList<>();

    public CharacterDTO() {}
}
