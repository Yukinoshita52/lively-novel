package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 内部伏笔记录，用于后续场景维持线索状态。
 */
@Getter
@Setter
public class ForeshadowDTO {
    private String scene;
    private String clue;
    private String status;
    private String note;
}
