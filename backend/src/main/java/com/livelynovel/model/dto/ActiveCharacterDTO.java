package com.livelynovel.model.dto;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * 近期活跃人物状态，用于下一场上下文选择，不进入最终 YAML。
 */
@Getter
@Setter
public class ActiveCharacterDTO {
    private String name;
    private String recentState;
    private String longTermRole;
    private String lastSeenScene;
    private List<String> unresolvedEvents = new ArrayList<>();

}
