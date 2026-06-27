package com.livelynovel.service;

import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.dto.ScreenplayConversionDetailDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 整本剧本转换服务。
 */
public interface ScreenplayService {

    SseEmitter convertNovel(String novelId, ScreenplayTypeEnum screenplayType);

    String getLatestConversionId(String novelId, ScreenplayTypeEnum screenplayType);

    ScreenplayConversionDetailDTO getLatestConversionSession(String novelId, ScreenplayTypeEnum screenplayType);

    ScreenplayConversionDetailDTO getConversionDetail(String conversionId);

    SceneDTO updatePersistedScene(String conversionId, int chapterIndex, int sceneIndexInChapter, SceneDTO scene);

    String exportConversionYaml(String conversionId);
}
