package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 人物关系 DTO。
 * 对应 yaml-schema.md §5.2 characters.relationships 字段。
 */
@Getter
@Setter
public class CharacterRelationshipDTO {
    private String target;
    private String relation;

    public CharacterRelationshipDTO() {}

    public CharacterRelationshipDTO(String target, String relation) {
        this.target = target;
        this.relation = relation;
    }
}
