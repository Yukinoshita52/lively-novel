package com.livelynovel.model.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 上传小说并落库后的响应体。
 */
@Getter
@Setter
public class NovelUploadResultDTO {

    private String novelId;
    private String title;
    private String contentHash;
    private int totalChapters;
    private int totalWordCount;
    private List<ChapterDTO> chapters;
}
