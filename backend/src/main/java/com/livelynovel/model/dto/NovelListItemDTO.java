package com.livelynovel.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 历史小说列表中的单项摘要。
 */
@Getter
@Setter
public class NovelListItemDTO {

    private String novelId;
    private String title;
    private int totalChapters;
    private int totalWordCount;
    private String createdAt;
    private String latestConversionId;
    private String latestConversionType;
    private String latestConversionStatus;
    private String latestConversionUpdatedAt;
    private String latestConversionErrorMessage;
}
