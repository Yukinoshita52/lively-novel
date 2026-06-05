package com.livelynovel.model.dto;

/**
 * 人物关系 DTO。
 * 对应 yaml-schema.md §5.2 characters.relationships 字段。
 */
public class CharacterRelationshipDTO {
    private String target;
    private String relation;

    public CharacterRelationshipDTO() {}

    public CharacterRelationshipDTO(String target, String relation) {
        this.target = target;
        this.relation = relation;
    }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }
}
