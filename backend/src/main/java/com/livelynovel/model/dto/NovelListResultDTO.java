package com.livelynovel.model.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 历史小说列表响应体。
 */
@Getter
@Setter
public class NovelListResultDTO {

    private List<NovelListItemDTO> novels;
    private int total;
}
