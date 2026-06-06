package com.livelynovel.service.impl;

import com.livelynovel.model.dto.ChapterSegmentDTO;
import com.livelynovel.model.dto.ChapterPreviewDTO;
import com.livelynovel.model.dto.NovelChapterDetailDTO;
import com.livelynovel.model.dto.NovelChaptersResultDTO;
import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.dto.SceneUnitDTO;
import com.livelynovel.service.ChapterSegmentationService;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.service.LlmService;
import com.livelynovel.service.NovelService;
import com.livelynovel.service.ScreenplayService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 整本转换骨架实现。
 */
@Service
public class ScreenplayServiceImpl implements ScreenplayService {

    private final NovelService novelService;
    private final LlmService llmService;
    private final ChapterSegmentationService chapterSegmentationService;

    public ScreenplayServiceImpl(
            NovelService novelService,
            LlmService llmService,
            ChapterSegmentationService chapterSegmentationService
    ) {
        this.novelService = novelService;
        this.llmService = llmService;
        this.chapterSegmentationService = chapterSegmentationService;
    }

    @Override
    public SseEmitter convertNovel(String novelId, ScreenplayTypeEnum screenplayType) {
        SseEmitter emitter = new SseEmitter(0L);

        CompletableFuture.runAsync(() -> convertNovel(novelId, screenplayType, emitter));

        return emitter;
    }

    void convertNovel(String novelId, ScreenplayTypeEnum screenplayType, SseEmitter emitter) {
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

            List<ChapterPreviewDTO> chapters = chaptersResult.getChapters();
            if (chapters == null) {
                chapters = Collections.emptyList();
            }

            for (ChapterPreviewDTO chapter : chapters) {
                if (chapter == null) {
                    continue;
                }

                NovelChapterDetailDTO detail = novelService.getChapterDetail(novelId, chapter.getChapterIndex());
                if (detail == null || detail.getContent() == null || detail.getContent().isBlank()) {
                    continue;
                }

                sendEvent(emitter, "chapter_loaded", Map.of(
                        "chapterIndex", detail.getChapterIndex(),
                        "title", detail.getTitle()
                ));

                List<SceneUnitDTO> sceneUnits = llmService.splitChapterIntoScenes(
                        detail.getContent(),
                        detail.getChapterIndex(),
                        screenplayType
                );
                if (sceneUnits == null) {
                    sceneUnits = Collections.emptyList();
                }
                sendEvent(emitter, "chapter_split", Map.of(
                        "chapterIndex", detail.getChapterIndex(),
                        "title", detail.getTitle(),
                        "sceneCount", sceneUnits.size()
                ));

                List<String> sceneTexts = rebuildSceneTexts(
                        chapterSegmentationService.segment(detail.getContent()),
                        detail.getChapterIndex(),
                        sceneUnits
                );
                for (int i = 0; i < sceneUnits.size(); i++) {
                    SceneUnitDTO sceneUnit = sceneUnits.get(i);
                    String sceneText = sceneTexts.get(i);
                    SceneDTO scene = llmService.convertSingleScene(sceneText, screenplayType);
                    scene.setSourceChapter(detail.getChapterIndex());
                    scene.setSourceText(sceneText);
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("chapterIndex", detail.getChapterIndex());
                    payload.put("sceneIndexInChapter", sceneUnit.getSceneIndexInChapter());
                    payload.put("title", sceneUnit.getTitle());
                    payload.put("sceneCount", sceneUnits.size());
                    payload.put("scene", scene);
                    sendEvent(emitter, "scene_completed", payload);
                }
            }

            sendEvent(emitter, "completed", Map.of(
                    "novelId", novelId,
                    "totalChapters", chaptersResult.getTotalChapters()
            ));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object payload) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(payload));
    }

    static List<String> rebuildSceneTexts(
            List<ChapterSegmentDTO> segments,
            int chapterIndex,
            List<SceneUnitDTO> sceneUnits
    ) {
        if (sceneUnits == null || sceneUnits.isEmpty()) {
            return Collections.emptyList();
        }
        if (segments == null || segments.isEmpty()) {
            throw new IllegalStateException("章节片段为空：chapterIndex=" + chapterIndex);
        }

        List<String> sceneTexts = new java.util.ArrayList<>(sceneUnits.size());
        for (SceneUnitDTO sceneUnit : sceneUnits) {
            String sceneText = resolveSceneText(segments, sceneUnit, chapterIndex);
            if (sceneText.isBlank()) {
                throw new IllegalStateException("场景原文为空：chapterIndex=" + chapterIndex
                        + ", sceneIndexInChapter=" + sceneUnit.getSceneIndexInChapter());
            }
            sceneTexts.add(sceneText);
        }
        return sceneTexts;
    }

    private static String resolveSceneText(
            List<ChapterSegmentDTO> segments,
            SceneUnitDTO sceneUnit,
            int chapterIndex
    ) {
        int startSegmentIndex = sceneUnit.getStartSegmentIndex();
        int endSegmentIndex = sceneUnit.getEndSegmentIndex();
        if (startSegmentIndex <= 0) {
            throw new IllegalStateException("起始片段非法：chapterIndex=" + chapterIndex
                    + ", sceneIndexInChapter=" + sceneUnit.getSceneIndexInChapter()
                    + ", startSegmentIndex=" + startSegmentIndex);
        }
        if (endSegmentIndex < startSegmentIndex) {
            throw new IllegalStateException("结束片段非法：chapterIndex=" + chapterIndex
                    + ", sceneIndexInChapter=" + sceneUnit.getSceneIndexInChapter()
                    + ", endSegmentIndex=" + endSegmentIndex);
        }
        if (endSegmentIndex > segments.size()) {
            throw new IllegalStateException("结束片段越界：chapterIndex=" + chapterIndex
                    + ", sceneIndexInChapter=" + sceneUnit.getSceneIndexInChapter()
                    + ", endSegmentIndex=" + endSegmentIndex);
        }

        List<String> texts = new java.util.ArrayList<>();
        for (int index = startSegmentIndex - 1; index < endSegmentIndex; index++) {
            texts.add(segments.get(index).getText());
        }
        return String.join("\n", texts).strip();
    }
}
