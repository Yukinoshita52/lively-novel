package com.livelynovel.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.livelynovel.model.dto.ChapterSegmentDTO;
import com.livelynovel.model.dto.ChapterPreviewDTO;
import com.livelynovel.model.dto.NovelChapterDetailDTO;
import com.livelynovel.model.dto.NovelChaptersResultDTO;
import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.dto.SceneUnitDTO;
import com.livelynovel.model.dto.ScreenplayConversionDetailDTO;
import com.livelynovel.model.dto.ScreenplayPersistedSceneDTO;
import com.livelynovel.model.entity.ScreenplayConversionEntity;
import com.livelynovel.model.entity.ScreenplaySceneEntity;
import com.livelynovel.model.entity.ScreenplaySceneUnitEntity;
import com.livelynovel.service.ChapterSegmentationService;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.repository.ScreenplayConversionRepository;
import com.livelynovel.repository.ScreenplaySceneRepository;
import com.livelynovel.repository.ScreenplaySceneUnitRepository;
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
import java.util.UUID;

/**
 * 整本转换骨架实现。
 */
@Service
public class ScreenplayServiceImpl implements ScreenplayService {

    private static final int MAX_SCENE_SPLIT_ATTEMPTS = 2;
    private static final int MULTI_SCENE_SEGMENT_THRESHOLD = 6;
    private static final String CONVERT_FAILED_MESSAGE = "转换未完成，请调整文本后重试。";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(
            YAMLFactory.builder()
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                    .build()
    );

    private final NovelService novelService;
    private final LlmService llmService;
    private final ChapterSegmentationService chapterSegmentationService;
    private final ScreenplayConversionRepository conversionRepository;
    private final ScreenplaySceneUnitRepository sceneUnitRepository;
    private final ScreenplaySceneRepository sceneRepository;

    public ScreenplayServiceImpl(
            NovelService novelService,
            LlmService llmService,
            ChapterSegmentationService chapterSegmentationService,
            ScreenplayConversionRepository conversionRepository,
            ScreenplaySceneUnitRepository sceneUnitRepository,
            ScreenplaySceneRepository sceneRepository
    ) {
        this.novelService = novelService;
        this.llmService = llmService;
        this.chapterSegmentationService = chapterSegmentationService;
        this.conversionRepository = conversionRepository;
        this.sceneUnitRepository = sceneUnitRepository;
        this.sceneRepository = sceneRepository;
    }

    @Override
    public SseEmitter convertNovel(String novelId, ScreenplayTypeEnum screenplayType) {
        SseEmitter emitter = new SseEmitter(0L);

        CompletableFuture.runAsync(() -> convertNovel(novelId, screenplayType, emitter));

        return emitter;
    }

    void convertNovel(String novelId, ScreenplayTypeEnum screenplayType, SseEmitter emitter) {
        java.util.Optional<ScreenplayConversionEntity> reusableConversion = java.util.Optional.ofNullable(
                conversionRepository.findFirstByNovelIdAndScreenplayTypeAndStatusInOrderByUpdatedAtDesc(
                        novelId,
                        screenplayType,
                        List.of("RUNNING", "FAILED", "COMPLETED")
                )
        ).orElse(java.util.Optional.empty());
        ScreenplayConversionEntity conversion = reusableConversion
                .orElseGet(() -> createConversion(novelId, screenplayType));
        boolean alreadyCompleted = "COMPLETED".equals(conversion.getStatus());
        if (reusableConversion.isPresent() && !alreadyCompleted) {
            conversion.setStatus("RUNNING");
            conversion.setErrorMessage(null);
            conversionRepository.save(conversion);
        }
        try {
            NovelChaptersResultDTO chaptersResult = novelService.getChapters(novelId);
            if (chaptersResult == null) {
                markConversionFailed(conversion, "小说不存在");
                completeWithFailedEvent(emitter, conversion.getId(), "小说不存在");
                return;
            }

            sendEvent(emitter, "started", Map.of(
                    "conversionId", conversion.getId(),
                    "novelId", novelId,
                    "title", chaptersResult.getTitle(),
                    "totalChapters", chaptersResult.getTotalChapters()
            ));

            List<ChapterPreviewDTO> chapters = chaptersResult.getChapters();
            if (chapters == null) {
                chapters = Collections.emptyList();
            }

            List<ScreenplaySceneUnitEntity> persistedSceneUnits = sceneUnitRepository
                    .findByConversionIdOrderByChapterIndexAscSceneIndexInChapterAsc(conversion.getId());
            List<ScreenplaySceneEntity> persistedScenes = sceneRepository
                    .findByConversionIdOrderByChapterIndexAscSceneIndexInChapterAsc(conversion.getId());
            if (persistedSceneUnits == null) {
                persistedSceneUnits = Collections.emptyList();
            }
            if (persistedScenes == null) {
                persistedScenes = Collections.emptyList();
            }

            for (ChapterPreviewDTO chapter : chapters) {
                if (chapter == null) {
                    continue;
                }

                List<ScreenplaySceneUnitEntity> chapterSceneUnits = persistedSceneUnits.stream()
                        .filter(sceneUnit -> sceneUnit.getChapterIndex() == chapter.getChapterIndex())
                        .toList();
                boolean replayedChapterSplit = !chapterSceneUnits.isEmpty();
                List<SceneUnitDTO> sceneUnits;
                List<String> sceneTexts;
                int chapterIndex;
                String chapterTitle;

                if (replayedChapterSplit) {
                    chapterIndex = chapter.getChapterIndex();
                    chapterTitle = chapter.getTitle();
                    sendEvent(emitter, "chapter_loaded", Map.of(
                            "conversionId", conversion.getId(),
                            "chapterIndex", chapterIndex,
                            "title", chapterTitle,
                            "replayed", true,
                            "message", "已载入历史章节"
                    ));
                    sceneUnits = chapterSceneUnits.stream()
                            .map(this::toSceneUnit)
                            .toList();
                    sceneTexts = chapterSceneUnits.stream()
                            .map(ScreenplaySceneUnitEntity::getSourceText)
                            .toList();
                    sendEvent(emitter, "chapter_split", Map.of(
                            "conversionId", conversion.getId(),
                            "chapterIndex", chapterIndex,
                            "title", chapterTitle,
                            "sceneCount", sceneUnits.size(),
                            "replayed", true,
                            "message", "已载入历史切场"
                    ));
                } else {
                    NovelChapterDetailDTO detail = novelService.getChapterDetail(novelId, chapter.getChapterIndex());
                    if (detail == null || detail.getContent() == null || detail.getContent().isBlank()) {
                        continue;
                    }
                    chapterIndex = detail.getChapterIndex();
                    chapterTitle = detail.getTitle();

                    sendEvent(emitter, "chapter_loaded", Map.of(
                            "conversionId", conversion.getId(),
                            "chapterIndex", chapterIndex,
                            "title", chapterTitle
                    ));

                    List<ChapterSegmentDTO> segments = chapterSegmentationService.segment(detail.getContent());
                    sceneUnits = splitChapterIntoScenesWithRetry(
                            detail.getContent(),
                            chapterIndex,
                            screenplayType,
                            segments
                    );
                    sendEvent(emitter, "chapter_split", Map.of(
                            "conversionId", conversion.getId(),
                            "chapterIndex", chapterIndex,
                            "title", chapterTitle,
                            "sceneCount", sceneUnits.size()
                    ));

                    sceneTexts = rebuildSceneTexts(
                            segments,
                            chapterIndex,
                            sceneUnits
                    );
                    persistSceneUnits(conversion.getId(), sceneUnits, sceneTexts);
                }

                for (int i = 0; i < sceneUnits.size(); i++) {
                    SceneUnitDTO sceneUnit = sceneUnits.get(i);
                    String sceneText = sceneTexts.get(i);
                    ScreenplaySceneEntity persistedScene = findPersistedScene(
                            persistedScenes,
                            chapterIndex,
                            sceneUnit.getSceneIndexInChapter()
                    );
                    if (persistedScene != null) {
                        sendSceneCompletedEvent(
                                emitter,
                                conversion.getId(),
                                chapterIndex,
                                sceneUnit,
                                fromJson(persistedScene.getSceneJson()),
                                sceneUnits.size(),
                                true
                        );
                        continue;
                    }

                    SceneDTO scene = llmService.convertSingleScene(sceneText, screenplayType);
                    scene.setSourceChapter(chapterIndex);
                    scene.setSourceText(sceneText);
                    persistGeneratedScene(conversion.getId(), chapterIndex, sceneUnit, scene);
                    sendSceneCompletedEvent(
                            emitter,
                            conversion.getId(),
                            chapterIndex,
                            sceneUnit,
                            scene,
                            sceneUnits.size(),
                            false
                    );
                }
            }

            if (!alreadyCompleted) {
                markConversionCompleted(conversion);
            }
            sendEvent(emitter, "completed", Map.of(
                    "conversionId", conversion.getId(),
                    "novelId", novelId,
                    "totalChapters", chaptersResult.getTotalChapters()
            ));
            emitter.complete();
        } catch (Exception e) {
            markConversionFailed(conversion, e.getMessage());
            completeWithFailedEvent(emitter, conversion.getId(), e.getMessage());
        }
    }

    private ScreenplayConversionEntity createConversion(String novelId, ScreenplayTypeEnum screenplayType) {
        ScreenplayConversionEntity conversion = new ScreenplayConversionEntity();
        conversion.setId(generateConversionId());
        conversion.setNovelId(novelId);
        conversion.setScreenplayType(screenplayType);
        conversion.setStatus("RUNNING");
        return conversionRepository.save(conversion);
    }

    private void markConversionCompleted(ScreenplayConversionEntity conversion) {
        conversion.setStatus("COMPLETED");
        conversion.setErrorMessage(null);
        conversionRepository.save(conversion);
    }

    private void markConversionFailed(ScreenplayConversionEntity conversion, String reason) {
        conversion.setStatus("FAILED");
        conversion.setErrorMessage(reason == null || reason.isBlank() ? "未知错误" : reason);
        conversionRepository.save(conversion);
    }

    private void persistSceneUnits(String conversionId, List<SceneUnitDTO> sceneUnits, List<String> sceneTexts) {
        List<ScreenplaySceneUnitEntity> entities = new java.util.ArrayList<>(sceneUnits.size());
        for (int i = 0; i < sceneUnits.size(); i++) {
            SceneUnitDTO sceneUnit = sceneUnits.get(i);
            ScreenplaySceneUnitEntity entity = new ScreenplaySceneUnitEntity();
            entity.setConversionId(conversionId);
            entity.setChapterIndex(sceneUnit.getSourceChapter());
            entity.setSceneIndexInChapter(sceneUnit.getSceneIndexInChapter());
            entity.setTitle(sceneUnit.getTitle());
            entity.setSummary(sceneUnit.getSummary());
            entity.setStartSegmentIndex(sceneUnit.getStartSegmentIndex());
            entity.setEndSegmentIndex(sceneUnit.getEndSegmentIndex());
            entity.setSourceText(sceneTexts.get(i));
            entity.setStatus("SPLIT");
            entities.add(entity);
        }
        sceneUnitRepository.saveAll(entities);
    }

    private void persistGeneratedScene(
            String conversionId,
            int chapterIndex,
            SceneUnitDTO sceneUnit,
            SceneDTO scene
    ) {
        ScreenplaySceneEntity entity = new ScreenplaySceneEntity();
        entity.setConversionId(conversionId);
        entity.setChapterIndex(chapterIndex);
        entity.setSceneIndexInChapter(sceneUnit.getSceneIndexInChapter());
        entity.setSceneJson(toJson(scene));
        sceneRepository.save(entity);
    }

    private SceneUnitDTO toSceneUnit(ScreenplaySceneUnitEntity entity) {
        SceneUnitDTO dto = new SceneUnitDTO();
        dto.setSourceChapter(entity.getChapterIndex());
        dto.setSceneIndexInChapter(entity.getSceneIndexInChapter());
        dto.setTitle(entity.getTitle());
        dto.setSummary(entity.getSummary());
        dto.setStartSegmentIndex(entity.getStartSegmentIndex());
        dto.setEndSegmentIndex(entity.getEndSegmentIndex());
        return dto;
    }

    private ScreenplaySceneEntity findPersistedScene(
            List<ScreenplaySceneEntity> persistedScenes,
            int chapterIndex,
            int sceneIndexInChapter
    ) {
        return persistedScenes.stream()
                .filter(scene -> scene.getChapterIndex() == chapterIndex)
                .filter(scene -> scene.getSceneIndexInChapter() == sceneIndexInChapter)
                .findFirst()
                .orElse(null);
    }

    private void sendSceneCompletedEvent(
            SseEmitter emitter,
            String conversionId,
            int chapterIndex,
            SceneUnitDTO sceneUnit,
            SceneDTO scene,
            int sceneCount,
            boolean replayed
    ) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("conversionId", conversionId);
        payload.put("chapterIndex", chapterIndex);
        payload.put("sceneIndexInChapter", sceneUnit.getSceneIndexInChapter());
        payload.put("title", sceneUnit.getTitle());
        payload.put("sceneCount", sceneCount);
        payload.put("scene", scene);
        if (replayed) {
            payload.put("replayed", true);
            payload.put("message", "已载入历史场景");
        }
        sendEvent(emitter, "scene_completed", payload);
    }

    private String toJson(SceneDTO scene) {
        try {
            return OBJECT_MAPPER.writeValueAsString(scene);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("场景持久化序列化失败", e);
        }
    }

    private String generateConversionId() {
        return "cv-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @Override
    public ScreenplayConversionDetailDTO getConversionDetail(String conversionId) {
        if (conversionId == null || conversionId.isBlank()) {
            return null;
        }
        return conversionRepository.findById(conversionId)
                .map(this::toConversionDetail)
                .orElse(null);
    }

    @Override
    public ScreenplayConversionDetailDTO getLatestCompletedConversion(String novelId, ScreenplayTypeEnum screenplayType) {
        if (novelId == null || novelId.isBlank() || screenplayType == null) {
            return null;
        }
        return conversionRepository
                .findFirstByNovelIdAndScreenplayTypeAndStatusOrderByUpdatedAtDesc(
                        novelId,
                        screenplayType,
                        "COMPLETED"
                )
                .map(this::toConversionDetail)
                .orElse(null);
    }

    @Override
    public SceneDTO updatePersistedScene(
            String conversionId,
            int chapterIndex,
            int sceneIndexInChapter,
            SceneDTO scene
    ) {
        if (conversionId == null || conversionId.isBlank() || scene == null) {
            return null;
        }
        return sceneRepository
                .findByConversionIdAndChapterIndexAndSceneIndexInChapter(
                        conversionId,
                        chapterIndex,
                        sceneIndexInChapter
                )
                .map(entity -> {
                    entity.setSceneJson(toJson(scene));
                    sceneRepository.save(entity);
                    return scene;
                })
                .orElse(null);
    }

    @Override
    public String exportConversionYaml(String conversionId) {
        ScreenplayConversionDetailDTO detail = getConversionDetail(conversionId);
        if (detail == null) {
            return null;
        }

        Map<String, Object> screenplay = new LinkedHashMap<>();
        screenplay.put("schemaVersion", "1.0");
        screenplay.put("title", "");
        screenplay.put("screenplayType", detail.getScreenplayType());
        screenplay.put("plotSummary", "");
        screenplay.put("characters", List.of());
        screenplay.put("scenes", detail.getScenes().stream()
                .map(ScreenplayPersistedSceneDTO::getScene)
                .toList());
        screenplay.put("storylines", List.of());
        return toYaml(screenplay);
    }

    private String toYaml(Map<String, Object> screenplay) {
        try {
            return YAML_MAPPER.writeValueAsString(screenplay);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("剧本 YAML 导出失败", e);
        }
    }

    private ScreenplayConversionDetailDTO toConversionDetail(ScreenplayConversionEntity conversion) {
        ScreenplayConversionDetailDTO detail = new ScreenplayConversionDetailDTO();
        detail.setConversionId(conversion.getId());
        detail.setNovelId(conversion.getNovelId());
        detail.setScreenplayType(conversion.getScreenplayType());
        detail.setStatus(conversion.getStatus());
        detail.setErrorMessage(conversion.getErrorMessage());

        List<ScreenplayPersistedSceneDTO> scenes = sceneRepository
                .findByConversionIdOrderByChapterIndexAscSceneIndexInChapterAsc(conversion.getId())
                .stream()
                .map(this::toPersistedScene)
                .toList();
        detail.setScenes(scenes);
        return detail;
    }

    private ScreenplayPersistedSceneDTO toPersistedScene(ScreenplaySceneEntity entity) {
        ScreenplayPersistedSceneDTO persistedScene = new ScreenplayPersistedSceneDTO();
        persistedScene.setChapterIndex(entity.getChapterIndex());
        persistedScene.setSceneIndexInChapter(entity.getSceneIndexInChapter());
        persistedScene.setScene(fromJson(entity.getSceneJson()));
        return persistedScene;
    }

    private SceneDTO fromJson(String sceneJson) {
        try {
            return OBJECT_MAPPER.readValue(sceneJson, SceneDTO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("场景持久化反序列化失败", e);
        }
    }

    private void completeWithFailedEvent(SseEmitter emitter, String conversionId, String reason) {
        try {
            sendEvent(emitter, "failed", Map.of(
                    "conversionId", conversionId,
                    "message", CONVERT_FAILED_MESSAGE,
                    "reason", reason == null || reason.isBlank() ? "未知错误" : reason
            ));
            emitter.complete();
        } catch (IOException sendError) {
            emitter.completeWithError(sendError);
        }
    }

    private List<SceneUnitDTO> splitChapterIntoScenesWithRetry(
            String chapterText,
            int chapterIndex,
            ScreenplayTypeEnum screenplayType,
            List<ChapterSegmentDTO> segments
    ) {
        IllegalStateException lastError = null;
        for (int attempt = 1; attempt <= MAX_SCENE_SPLIT_ATTEMPTS; attempt++) {
            List<SceneUnitDTO> sceneUnits = llmService.splitChapterIntoScenes(
                    chapterText,
                    chapterIndex,
                    screenplayType
            );
            if (sceneUnits == null) {
                sceneUnits = Collections.emptyList();
            }
            try {
                validateSceneUnits(sceneUnits, chapterIndex, segments.size());
                return sceneUnits;
            } catch (IllegalStateException e) {
                lastError = e;
            }
        }
        throw lastError;
    }

    private static void validateSceneUnits(List<SceneUnitDTO> sceneUnits, int chapterIndex, int totalSegments) {
        if (totalSegments <= 0) {
            throw new IllegalStateException("章节片段为空：chapterIndex=" + chapterIndex);
        }
        if (sceneUnits == null || sceneUnits.isEmpty()) {
            throw new IllegalStateException("章节切场结果为空：chapterIndex=" + chapterIndex);
        }
        if (sceneUnits.size() == 1 && totalSegments >= MULTI_SCENE_SEGMENT_THRESHOLD) {
            SceneUnitDTO sceneUnit = sceneUnits.get(0);
            if (sceneUnit.getStartSegmentIndex() == 1 && sceneUnit.getEndSegmentIndex() == totalSegments) {
                throw new IllegalStateException("章节切场退化为整章单场：chapterIndex=" + chapterIndex
                        + ", totalSegments=" + totalSegments);
            }
        }

        int expectedSceneIndex = 1;
        int expectedStartSegmentIndex = 1;
        for (SceneUnitDTO sceneUnit : sceneUnits) {
            if (sceneUnit == null) {
                throw new IllegalStateException("场景切分为空：chapterIndex=" + chapterIndex
                        + ", sceneIndexInChapter=" + expectedSceneIndex);
            }
            if (sceneUnit.getSceneIndexInChapter() != expectedSceneIndex) {
                throw new IllegalStateException("场景编号不连续：chapterIndex=" + chapterIndex
                        + ", expectedSceneIndex=" + expectedSceneIndex
                        + ", sceneIndexInChapter=" + sceneUnit.getSceneIndexInChapter());
            }

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
            if (endSegmentIndex > totalSegments) {
                throw new IllegalStateException("结束片段越界：chapterIndex=" + chapterIndex
                        + ", sceneIndexInChapter=" + sceneUnit.getSceneIndexInChapter()
                        + ", endSegmentIndex=" + endSegmentIndex);
            }
            if (startSegmentIndex != expectedStartSegmentIndex) {
                throw new IllegalStateException("片段不连续：chapterIndex=" + chapterIndex
                        + ", sceneIndexInChapter=" + sceneUnit.getSceneIndexInChapter()
                        + ", expectedStartSegmentIndex=" + expectedStartSegmentIndex
                        + ", startSegmentIndex=" + startSegmentIndex);
            }

            expectedSceneIndex++;
            expectedStartSegmentIndex = endSegmentIndex + 1;
        }
        if (expectedStartSegmentIndex != totalSegments + 1) {
            throw new IllegalStateException("片段未覆盖完整章节：chapterIndex=" + chapterIndex
                    + ", expectedStartSegmentIndex=" + expectedStartSegmentIndex
                    + ", totalSegments=" + totalSegments);
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object payload) throws IOException {
        Object eventPayload = payload;
        if (payload instanceof Map<?, ?> payloadMap) {
            Map<String, Object> enrichedPayload = new LinkedHashMap<>();
            enrichedPayload.put("eventName", eventName);
            for (Map.Entry<?, ?> entry : payloadMap.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    enrichedPayload.put(key, entry.getValue());
                }
            }
            eventPayload = enrichedPayload;
        }
        emitter.send(SseEmitter.event().name(eventName).data(eventPayload));
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
