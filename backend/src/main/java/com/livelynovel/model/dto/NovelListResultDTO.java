package com.livelynovel.model.dto;

import java.util.List;

/**
 * 历史小说列表响应体。
 */
public class NovelListResultDTO {

    private List<NovelListItemDTO> novels;
    private int total;

    public List<NovelListItemDTO> getNovels() {
        return novels;
    }

    public void setNovels(List<NovelListItemDTO> novels) {
        this.novels = novels;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
