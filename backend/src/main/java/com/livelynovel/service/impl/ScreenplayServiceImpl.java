package com.livelynovel.service.impl;

import com.livelynovel.model.dto.ChapterPreviewDTO;
import com.livelynovel.model.dto.NovelChapterDetailDTO;
import com.livelynovel.model.dto.NovelChaptersResultDTO;
import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.service.LlmService;
import com.livelynovel.service.NovelService;
import com.livelynovel.service.ScreenplayService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 整本转换骨架实现。
 */
@Service
public class ScreenplayServiceImpl implements ScreenplayService {

    private final NovelService novelService;
    private final LlmService llmService;

    public ScreenplayServiceImpl(NovelService novelService, LlmService llmService) {
        this.novelService = novelService;
        this.llmService = llmService;
    }

    @Override
    public SseEmitter convertNovel(String novelId, ScreenplayTypeEnum screenplayType) {
        SseEmitter emitter = new SseEmitter(0L);

        CompletableFuture.runAsync(() -> {
            try {
                NovelChaptersResultDTO chaptersResult = novelService.getChapters(novelId);
                if (chaptersResult == null) {
                    emitter.completeWithError(new IllegalArgumentException("小说不存在"));
                    return;
                }

                sendEvent(emitter, "started", Map.of(
                        "novelId", novelId,
                        "title", chaptersResult.getTitle(),
                        "totalChapters", chaptersResult.getTotalChapters()
                ));

                for (ChapterPreviewDTO chapter : chaptersResult.getChapters()) {
                    sendEvent(emitter, "chapter_loaded", Map.of(
                            "chapterIndex", chapter.getChapterIndex(),
                            "title", chapter.getTitle()
                    ));

                    NovelChapterDetailDTO detail = novelService.getChapterDetail(novelId, chapter.getChapterIndex());
                    if (detail == null || detail.getContent() == null || detail.getContent().isBlank()) {
                        continue;
                    }

                    SceneDTO scene = llmService.convertSingleScene(detail.getContent(), screenplayType);
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("chapterIndex", detail.getChapterIndex());
                    payload.put("title", detail.getTitle());
                    payload.put("sceneId", scene.getSceneId());
                    payload.put("heading", scene.getHeading());
                    sendEvent(emitter, "scene_generated", payload);
                }

                sendEvent(emitter, "completed", Map.of(
                        "novelId", novelId,
                        "totalChapters", chaptersResult.getTotalChapters()
                ));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object payload) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(payload));
    }
}
