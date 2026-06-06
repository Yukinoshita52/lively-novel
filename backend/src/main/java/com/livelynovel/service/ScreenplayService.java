package com.livelynovel.service;

import com.livelynovel.model.enums.ScreenplayTypeEnum;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 整本剧本转换服务。
 */
public interface ScreenplayService {

    SseEmitter convertNovel(String novelId, ScreenplayTypeEnum screenplayType);
}
