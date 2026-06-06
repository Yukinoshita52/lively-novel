package com.livelynovel.service;

import com.livelynovel.model.dto.ChapterDTO;

import java.util.List;

/**
 * 章节切分服务。
 */
public interface ChapterSplitter {

    List<ChapterDTO> split(String text);
}
