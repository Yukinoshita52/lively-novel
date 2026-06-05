package com.livelynovel.model.dto;

import com.livelynovel.model.enums.CharacterRoleEnum;
import java.util.ArrayList;
import java.util.List;

/**
 * 人物 DTO。
 * 对应 yaml-schema.md §5.2 characters 字段。
 */
public class CharacterDTO {
    private String name;
    private CharacterRoleEnum role;
    private String description;
    private String firstAppearance;
    private List<CharacterRelationshipDTO> relationships = new ArrayList<>();

    public CharacterDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CharacterRoleEnum getRole() { return role; }
    public void setRole(CharacterRoleEnum role) { this.role = role; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFirstAppearance() { return firstAppearance; }
    public void setFirstAppearance(String firstAppearance) { this.firstAppearance = firstAppearance; }
    public List<CharacterRelationshipDTO> getRelationships() { return relationships; }
    public void setRelationships(List<CharacterRelationshipDTO> relationships) { this.relationships = relationships; }
}
