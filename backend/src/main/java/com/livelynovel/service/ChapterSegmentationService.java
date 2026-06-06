package com.livelynovel.service;

import com.livelynovel.model.dto.ChapterSegmentDTO;

import java.util.List;

/**
 * 章节片段切分服务。
 */
public interface ChapterSegmentationService {

    /**
     * 将单章正文切成顺序编号片段。
     */
    List<ChapterSegmentDTO> segment(String chapterText);
}
