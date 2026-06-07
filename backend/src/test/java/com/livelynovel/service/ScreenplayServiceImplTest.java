package com.livelynovel.service;

import com.livelynovel.model.dto.ChapterPreviewDTO;
import com.livelynovel.model.dto.NovelChapterDetailDTO;
import com.livelynovel.model.dto.NovelChaptersResultDTO;
import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.dto.SceneUnitDTO;
import com.livelynovel.model.dto.ScreenplayConversionDetailDTO;
import com.livelynovel.model.entity.ScreenplayConversionEntity;
import com.livelynovel.model.entity.ScreenplaySceneEntity;
import com.livelynovel.model.entity.ScreenplaySceneUnitEntity;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.common.exception.SceneLanguageDriftException;
import com.livelynovel.repository.ScreenplayConversionRepository;
import com.livelynovel.repository.ScreenplaySceneRepository;
import com.livelynovel.repository.ScreenplaySceneUnitRepository;
import com.livelynovel.service.impl.ChapterSegmentationServiceImpl;
import com.livelynovel.service.impl.ScreenplayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScreenplayServiceImplTest {

    private final NovelService novelService = mock(NovelService.class);
    private final LlmService llmService = mock(LlmService.class);
    private final ScreenplayConversionRepository conversionRepository = mock(ScreenplayConversionRepository.class);
    private final ScreenplaySceneUnitRepository sceneUnitRepository = mock(ScreenplaySceneUnitRepository.class);
    private final ScreenplaySceneRepository sceneRepository = mock(ScreenplaySceneRepository.class);
    private final ChapterSegmentationService chapterSegmentationService = new ChapterSegmentationServiceImpl();
    private final ScreenplayServiceImpl screenplayService = new ScreenplayServiceImpl(
            novelService,
            llmService,
            chapterSegmentationService,
            conversionRepository,
            sceneUnitRepository,
            sceneRepository
    );

    @BeforeEach
    void setUp() {
        when(conversionRepository.save(org.mockito.ArgumentMatchers.any(ScreenplayConversionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(sceneUnitRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(sceneRepository.save(org.mockito.ArgumentMatchers.any(ScreenplaySceneEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(llmService.convertSingleScene(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(ScreenplayTypeEnum.ANIME)
        )).thenReturn(scene("scene-default"));
    }

    @Test
    void splitsChapterIntoMultipleSceneUnitsBeforeConvertingEachScene() throws Exception {
        String novelId = "nv-1234abcd";
        String chapterContent = "路灯在雨幕里发白。林秋站在站牌下，攥着书包带。\n"
                + "她抬头看向马路尽头，末班车还没来。\n"
                + "街角便利店的门被推开，周竞跑了出来，手里举着一把伞。\n"
                + "他停在她面前，气息很乱，说总算赶上了。\n"
                + "雨声渐渐小了，二人并肩往学校走去。\n"
                + "教学楼的走廊还亮着灯，值日生正在拖地。\n"
                + "林秋在公告栏前停下，盯着分班名单没有说话。\n"
                + "周竞顺着她的视线看过去，轻声问她是不是分到了别班。";
        SceneUnitDTO firstUnit = sceneUnit(1, 1, 1, 4);
        SceneUnitDTO secondUnit = sceneUnit(1, 2, 5, 8);
        CountDownLatch releaseGetChapters = new CountDownLatch(1);

        when(novelService.getChapters(novelId)).thenAnswer(invocation -> {
            releaseGetChapters.await(2, TimeUnit.SECONDS);
            return chaptersResult(novelId);
        });
        when(novelService.getChapterDetail(novelId, 1)).thenReturn(chapterDetail(novelId, chapterContent));
        when(llmService.splitChapterIntoScenes(chapterContent, 1, ScreenplayTypeEnum.ANIME))
            .thenReturn(List.of(firstUnit, secondUnit));
        when(llmService.convertSingleScene(
                "路灯在雨幕里发白。林秋站在站牌下，攥着书包带。\n"
                        + "她抬头看向马路尽头，末班车还没来。\n"
                        + "街角便利店的门被推开，周竞跑了出来，手里举着一把伞。\n"
                        + "他停在她面前，气息很乱，说总算赶上了。",
                ScreenplayTypeEnum.ANIME
        )).thenReturn(scene("scene-1"));
        when(llmService.convertSingleScene(
                "雨声渐渐小了，二人并肩往学校走去。\n"
                        + "教学楼的走廊还亮着灯，值日生正在拖地。\n"
                        + "林秋在公告栏前停下，盯着分班名单没有说话。\n"
                        + "周竞顺着她的视线看过去，轻声问她是不是分到了别班。",
                ScreenplayTypeEnum.ANIME
        )).thenReturn(scene("scene-2"));

        CountDownLatch completion = new CountDownLatch(1);
        SseEmitter emitter = screenplayService.convertNovel(novelId, ScreenplayTypeEnum.ANIME);
        initializeEmitterHandler(emitter, new CopyOnWriteArrayList<>(), completion, new AtomicReference<>());
        releaseGetChapters.countDown();
        assertThat(completion.await(2, TimeUnit.SECONDS)).isTrue();

        verify(llmService, times(1)).splitChapterIntoScenes(chapterContent, 1, ScreenplayTypeEnum.ANIME);
        verify(llmService, times(1)).convertSingleScene(
                "路灯在雨幕里发白。林秋站在站牌下，攥着书包带。\n"
                        + "她抬头看向马路尽头，末班车还没来。\n"
                        + "街角便利店的门被推开，周竞跑了出来，手里举着一把伞。\n"
                        + "他停在她面前，气息很乱，说总算赶上了。",
                ScreenplayTypeEnum.ANIME
        );
        verify(llmService, times(1)).convertSingleScene(
                "雨声渐渐小了，二人并肩往学校走去。\n"
                        + "教学楼的走廊还亮着灯，值日生正在拖地。\n"
                        + "林秋在公告栏前停下，盯着分班名单没有说话。\n"
                        + "周竞顺着她的视线看过去，轻声问她是不是分到了别班。",
                ScreenplayTypeEnum.ANIME
        );
    }

    @Test
    void emitsChapterSplitEventWithSceneCount() throws Exception {
        String novelId = "nv-1234abcd";
        String chapterContent = "第一段原文。\n第二段原文。\n第三段原文。";
        SceneUnitDTO firstUnit = sceneUnit(1, 1, 1, 2);
        SceneUnitDTO secondUnit = sceneUnit(1, 2, 3, 3);
        CountDownLatch releaseGetChapters = new CountDownLatch(1);
        List<Object> sentPayloads = new CopyOnWriteArrayList<>();

        when(novelService.getChapters(novelId)).thenAnswer(invocation -> {
            releaseGetChapters.await(2, TimeUnit.SECONDS);
            return chaptersResult(novelId);
        });
        when(novelService.getChapterDetail(novelId, 1)).thenReturn(chapterDetail(novelId, chapterContent));
        when(llmService.splitChapterIntoScenes(chapterContent, 1, ScreenplayTypeEnum.ANIME))
            .thenReturn(List.of(firstUnit, secondUnit));
        when(llmService.convertSingleScene("第一段原文。\n第二段原文", ScreenplayTypeEnum.ANIME)).thenReturn(scene("scene-1"));
        when(llmService.convertSingleScene("第三段原文", ScreenplayTypeEnum.ANIME)).thenReturn(scene("scene-2"));

        CountDownLatch completion = new CountDownLatch(1);
        SseEmitter emitter = screenplayService.convertNovel(novelId, ScreenplayTypeEnum.ANIME);
        initializeEmitterHandler(emitter, sentPayloads, completion, new AtomicReference<>());
        releaseGetChapters.countDown();
        assertThat(completion.await(2, TimeUnit.SECONDS)).isTrue();

        assertThat(containsChapterSplitEvent(sentPayloads)).isTrue();
        assertThat(containsSceneCount(sentPayloads, 2)).isTrue();
    }

    @Test
    void persistsConversionSceneUnitsAndGeneratedScenes() throws Exception {
        String novelId = "nv-1234abcd";
        String chapterContent = "第一段原文。\n第二段原文。\n第三段原文。";
        SceneUnitDTO firstUnit = sceneUnit(1, 1, 1, 2);
        SceneUnitDTO secondUnit = sceneUnit(1, 2, 3, 3);
        CountDownLatch releaseGetChapters = new CountDownLatch(1);
        List<Object> sentPayloads = new CopyOnWriteArrayList<>();

        when(novelService.getChapters(novelId)).thenAnswer(invocation -> {
            releaseGetChapters.await(2, TimeUnit.SECONDS);
            return chaptersResult(novelId);
        });
        when(novelService.getChapterDetail(novelId, 1)).thenReturn(chapterDetail(novelId, chapterContent));
        when(llmService.splitChapterIntoScenes(chapterContent, 1, ScreenplayTypeEnum.ANIME))
            .thenReturn(List.of(firstUnit, secondUnit));
        when(llmService.convertSingleScene("第一段原文。\n第二段原文", ScreenplayTypeEnum.ANIME)).thenReturn(scene("scene-1"));
        when(llmService.convertSingleScene("第三段原文", ScreenplayTypeEnum.ANIME)).thenReturn(scene("scene-2"));

        CountDownLatch completion = new CountDownLatch(1);
        SseEmitter emitter = screenplayService.convertNovel(novelId, ScreenplayTypeEnum.ANIME);
        initializeEmitterHandler(emitter, sentPayloads, completion, new AtomicReference<>());
        releaseGetChapters.countDown();
        assertThat(completion.await(2, TimeUnit.SECONDS)).isTrue();

        ArgumentCaptor<ScreenplayConversionEntity> conversionCaptor =
                ArgumentCaptor.forClass(ScreenplayConversionEntity.class);
        verify(conversionRepository, times(2)).save(conversionCaptor.capture());
        assertThat(conversionCaptor.getAllValues().get(0).getId()).startsWith("cv-");
        assertThat(conversionCaptor.getAllValues().get(0).getNovelId()).isEqualTo(novelId);
        assertThat(conversionCaptor.getAllValues().get(0).getScreenplayType()).isEqualTo(ScreenplayTypeEnum.ANIME);
        assertThat(conversionCaptor.getAllValues().get(1).getStatus()).isEqualTo("COMPLETED");

        ArgumentCaptor<List<ScreenplaySceneUnitEntity>> sceneUnitsCaptor = ArgumentCaptor.forClass(List.class);
        verify(sceneUnitRepository).saveAll(sceneUnitsCaptor.capture());
        assertThat(sceneUnitsCaptor.getValue()).hasSize(2);
        assertThat(sceneUnitsCaptor.getValue().get(0).getSourceText()).isEqualTo("第一段原文。\n第二段原文。");
        assertThat(sceneUnitsCaptor.getValue().get(1).getSourceText()).isEqualTo("第三段原文。");

        ArgumentCaptor<ScreenplaySceneEntity> sceneCaptor = ArgumentCaptor.forClass(ScreenplaySceneEntity.class);
        verify(sceneRepository, times(2)).save(sceneCaptor.capture());
        assertThat(sceneCaptor.getAllValues()).extracting(ScreenplaySceneEntity::getSceneJson)
                .allMatch(json -> json.toString().contains("\"sceneId\""));
        assertThat(containsConversionId(sentPayloads)).isTrue();
    }

    @Test
    void replaysCompletedConversionThroughSseWithoutCallingLlm() throws Exception {
        String novelId = "nv-1234abcd";
        ScreenplayConversionEntity conversion = conversion("cv-completed", novelId, "COMPLETED");
        ScreenplaySceneUnitEntity sceneUnit = sceneUnitEntity("cv-completed", 1, 1, "第一场", "第一段原文。");
        ScreenplaySceneEntity sceneEntity = sceneEntity("cv-completed", 1, 1, "scene-1");
        List<Object> sentPayloads = new CopyOnWriteArrayList<>();

        when(conversionRepository.findFirstByNovelIdAndScreenplayTypeAndStatusInOrderByUpdatedAtDesc(
                novelId,
                ScreenplayTypeEnum.ANIME,
                List.of("RUNNING", "FAILED", "COMPLETED")
        )).thenReturn(Optional.of(conversion));
        when(novelService.getChapters(novelId)).thenReturn(chaptersResult(novelId));
        when(sceneUnitRepository.findByConversionIdOrderByChapterIndexAscSceneIndexInChapterAsc("cv-completed"))
                .thenReturn(List.of(sceneUnit));
        when(sceneRepository.findByConversionIdOrderByChapterIndexAscSceneIndexInChapterAsc("cv-completed"))
                .thenReturn(List.of(sceneEntity));

        CountDownLatch completion = new CountDownLatch(1);
        SseEmitter emitter = screenplayService.convertNovel(novelId, ScreenplayTypeEnum.ANIME);
        initializeEmitterHandler(emitter, sentPayloads, completion, new AtomicReference<>());
        assertThat(completion.await(2, TimeUnit.SECONDS)).isTrue();

        verify(conversionRepository, never()).save(org.mockito.ArgumentMatchers.any(ScreenplayConversionEntity.class));
        verify(llmService, never()).splitChapterIntoScenes(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.eq(ScreenplayTypeEnum.ANIME)
        );
        verify(llmService, never()).convertSingleScene(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(ScreenplayTypeEnum.ANIME)
        );
        assertThat(containsReplayedEvent(sentPayloads, "chapter_split")).isTrue();
        assertThat(containsReplayedEvent(sentPayloads, "scene_completed")).isTrue();
        assertThat(containsEvent(sentPayloads, "completed")).isTrue();
    }

    @Test
    void resumesFromPersistedSceneUnitsAndConvertsOnlyMissingScenes() throws Exception {
        String novelId = "nv-1234abcd";
        ScreenplayConversionEntity conversion = conversion("cv-partial", novelId, "FAILED");
        ScreenplaySceneUnitEntity firstUnit = sceneUnitEntity("cv-partial", 1, 1, "第一场", "第一段原文。");
        ScreenplaySceneUnitEntity secondUnit = sceneUnitEntity("cv-partial", 1, 2, "第二场", "第二段原文。");
        ScreenplaySceneEntity firstScene = sceneEntity("cv-partial", 1, 1, "scene-1");
        List<Object> sentPayloads = new CopyOnWriteArrayList<>();

        when(conversionRepository.findFirstByNovelIdAndScreenplayTypeAndStatusInOrderByUpdatedAtDesc(
                novelId,
                ScreenplayTypeEnum.ANIME,
                List.of("RUNNING", "FAILED", "COMPLETED")
        )).thenReturn(Optional.of(conversion));
        when(novelService.getChapters(novelId)).thenReturn(chaptersResult(novelId));
        when(novelService.getChapterDetail(novelId, 1)).thenReturn(chapterDetail(novelId, "第一段原文。\n第二段原文。"));
        when(sceneUnitRepository.findByConversionIdOrderByChapterIndexAscSceneIndexInChapterAsc("cv-partial"))
                .thenReturn(List.of(firstUnit, secondUnit));
        when(sceneRepository.findByConversionIdOrderByChapterIndexAscSceneIndexInChapterAsc("cv-partial"))
                .thenReturn(List.of(firstScene));
        when(llmService.convertSingleScene("第二段原文。", ScreenplayTypeEnum.ANIME)).thenReturn(scene("scene-2"));

        CountDownLatch completion = new CountDownLatch(1);
        SseEmitter emitter = screenplayService.convertNovel(novelId, ScreenplayTypeEnum.ANIME);
        initializeEmitterHandler(emitter, sentPayloads, completion, new AtomicReference<>());
        assertThat(completion.await(2, TimeUnit.SECONDS)).isTrue();

        verify(llmService, never()).splitChapterIntoScenes(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.eq(ScreenplayTypeEnum.ANIME)
        );
        verify(llmService, times(1)).convertSingleScene("第二段原文。", ScreenplayTypeEnum.ANIME);
        verify(sceneRepository, times(1)).save(org.mockito.ArgumentMatchers.any(ScreenplaySceneEntity.class));
        assertThat(containsReplayedEvent(sentPayloads, "chapter_split")).isTrue();
        assertThat(containsReplayedEvent(sentPayloads, "scene_completed")).isTrue();
        assertThat(containsFreshScene(sentPayloads, 2)).isTrue();
    }

    @Test
    void returnsPersistedConversionDetailWithGeneratedScenes() {
        ScreenplayConversionEntity conversion = new ScreenplayConversionEntity();
        conversion.setId("cv-1234abcd");
        conversion.setNovelId("nv-1234abcd");
        conversion.setScreenplayType(ScreenplayTypeEnum.ANIME);
        conversion.setStatus("COMPLETED");

        ScreenplaySceneEntity sceneEntity = new ScreenplaySceneEntity();
        sceneEntity.setConversionId("cv-1234abcd");
        sceneEntity.setChapterIndex(1);
        sceneEntity.setSceneIndexInChapter(1);
        sceneEntity.setSceneJson("""
                {
                  "sceneId": "s1",
                  "heading": {"interior": true, "location": "教室", "timeOfDay": "午后"},
                  "actionLines": ["林秋把书包放在桌上。"],
                  "dialogueBlocks": [],
                  "visualizedInnerThoughts": [],
                  "transitions": [],
                  "sourceChapter": 1,
                  "sourceText": "第一段原文。"
                }
                """);

        ScreenplaySceneUnitEntity sceneUnitEntity = new ScreenplaySceneUnitEntity();
        sceneUnitEntity.setConversionId("cv-1234abcd");
        sceneUnitEntity.setChapterIndex(1);
        sceneUnitEntity.setSceneIndexInChapter(1);
        sceneUnitEntity.setTitle("特典：比食欲更加重要的东西");

        when(conversionRepository.findById("cv-1234abcd")).thenReturn(Optional.of(conversion));
        when(sceneRepository.findByConversionIdOrderByChapterIndexAscSceneIndexInChapterAsc("cv-1234abcd"))
                .thenReturn(List.of(sceneEntity));
        when(sceneUnitRepository.findByConversionIdOrderByChapterIndexAscSceneIndexInChapterAsc("cv-1234abcd"))
                .thenReturn(List.of(sceneUnitEntity));

        ScreenplayConversionDetailDTO detail = screenplayService.getConversionDetail("cv-1234abcd");

        assertThat(detail).isNotNull();
        assertThat(detail.getConversionId()).isEqualTo("cv-1234abcd");
        assertThat(detail.getNovelId()).isEqualTo("nv-1234abcd");
        assertThat(detail.getStatus()).isEqualTo("COMPLETED");
        assertThat(detail.getScenes()).hasSize(1);
        assertThat(detail.getScenes().get(0).getTitle()).isEqualTo("特典：比食欲更加重要的东西");
        assertThat(detail.getScenes().get(0).getScene().getSceneId()).isEqualTo("s1");
    }

    @Test
    void returnsLatestCompletedConversionByNovelAndType() {
        ScreenplayConversionEntity conversion = new ScreenplayConversionEntity();
        conversion.setId("cv-completed");
        conversion.setNovelId("nv-1234abcd");
        conversion.setScreenplayType(ScreenplayTypeEnum.ANIME);
        conversion.setStatus("COMPLETED");

        ScreenplaySceneEntity sceneEntity = new ScreenplaySceneEntity();
        sceneEntity.setConversionId("cv-completed");
        sceneEntity.setChapterIndex(1);
        sceneEntity.setSceneIndexInChapter(1);
        sceneEntity.setSceneJson("""
                {
                  "sceneId": "s1",
                  "heading": {"interior": true, "location": "教室", "timeOfDay": "午后"},
                  "actionLines": ["林秋把书包放在桌上。"],
                  "dialogueBlocks": [],
                  "visualizedInnerThoughts": [],
                  "transitions": [],
                  "sourceChapter": 1,
                  "sourceText": "第一段原文。"
                }
                """);

        when(conversionRepository.findFirstByNovelIdAndScreenplayTypeAndStatusOrderByUpdatedAtDesc(
                "nv-1234abcd",
                ScreenplayTypeEnum.ANIME,
                "COMPLETED"
        )).thenReturn(Optional.of(conversion));
        when(sceneRepository.findByConversionIdOrderByChapterIndexAscSceneIndexInChapterAsc("cv-completed"))
                .thenReturn(List.of(sceneEntity));

        ScreenplayConversionDetailDTO detail =
                screenplayService.getLatestCompletedConversion("nv-1234abcd", ScreenplayTypeEnum.ANIME);

        assertThat(detail).isNotNull();
        assertThat(detail.getConversionId()).isEqualTo("cv-completed");
        assertThat(detail.getStatus()).isEqualTo("COMPLETED");
        assertThat(detail.getScenes()).hasSize(1);
    }

    @Test
    void exportsScriptYamlWithoutSourceText() {
        ScreenplayConversionEntity conversion = new ScreenplayConversionEntity();
        conversion.setId("cv-1234abcd");
        conversion.setNovelId("nv-1234abcd");
        conversion.setScreenplayType(ScreenplayTypeEnum.ANIME);
        conversion.setStatus("COMPLETED");

        ScreenplaySceneEntity sceneEntity = new ScreenplaySceneEntity();
        sceneEntity.setConversionId("cv-1234abcd");
        sceneEntity.setChapterIndex(1);
        sceneEntity.setSceneIndexInChapter(1);
        sceneEntity.setSceneJson("""
                {
                  "sceneId": "s1",
                  "heading": {"interior": true, "location": "教室", "timeOfDay": "午后"},
                  "scriptBlocks": [
                    {"type": "SHOT", "text": "家庭餐厅一角。午后的光从窗边斜照进来。"},
                    {"type": "ACTION", "text": "林秋把书包放在桌上。"},
                    {"type": "INSERT", "text": "桌面上的自助饮料杯、大份薯条和轻小说最新卷。"},
                    {"type": "SFX", "text": "隔壁桌传来女声尖叫。"},
                    {"type": "VO", "character": "林秋", "parenthetical": "画外音", "line": "我还不能停在这里。"},
                    {"type": "DIALOGUE", "character": "林秋", "line": "我已经没事了。"},
                    {"type": "ACTION", "text": "她抬头看向窗外。"},
                    {"type": "TRANSITION", "text": "切至：走廊"}
                  ],
                  "sourceChapter": 1,
                  "sourceText": "第一段原文。"
                }
                """);

        when(conversionRepository.findById("cv-1234abcd")).thenReturn(Optional.of(conversion));
        when(sceneRepository.findByConversionIdOrderByChapterIndexAscSceneIndexInChapterAsc("cv-1234abcd"))
                .thenReturn(List.of(sceneEntity));

        String yaml = screenplayService.exportConversionYaml("cv-1234abcd");

        assertThat(yaml).contains("schemaVersion: \"1.0\"");
        assertThat(yaml).contains("screenplayType: \"ANIME\"");
        assertThat(yaml).contains("characters: []");
        assertThat(yaml).contains("storylines: []");
        assertThat(yaml).contains("scenes:");
        assertThat(yaml).contains("sceneId: \"s1\"");
        assertThat(yaml).contains("scriptBlocks:");
        assertThat(yaml).contains("type: \"SHOT\"");
        assertThat(yaml).contains("text: \"家庭餐厅一角。午后的光从窗边斜照进来。\"");
        assertThat(yaml).contains("type: \"ACTION\"");
        assertThat(yaml).contains("text: \"林秋把书包放在桌上。\"");
        assertThat(yaml).contains("type: \"INSERT\"");
        assertThat(yaml).contains("text: \"桌面上的自助饮料杯、大份薯条和轻小说最新卷。\"");
        assertThat(yaml).contains("type: \"SFX\"");
        assertThat(yaml).contains("text: \"隔壁桌传来女声尖叫。\"");
        assertThat(yaml).contains("type: \"VO\"");
        assertThat(yaml).contains("type: \"DIALOGUE\"");
        assertThat(yaml).contains("character: \"林秋\"");
        assertThat(yaml).contains("parenthetical: \"画外音\"");
        assertThat(yaml).contains("line: \"我还不能停在这里。\"");
        assertThat(yaml).contains("line: \"我已经没事了。\"");
        assertThat(yaml).containsSubsequence(
                "text: \"家庭餐厅一角。午后的光从窗边斜照进来。\"",
                "text: \"林秋把书包放在桌上。\"",
                "text: \"桌面上的自助饮料杯、大份薯条和轻小说最新卷。\"",
                "text: \"隔壁桌传来女声尖叫。\"",
                "line: \"我还不能停在这里。\"",
                "line: \"我已经没事了。\"",
                "text: \"她抬头看向窗外。\"",
                "text: \"切至：走廊\""
        );
        assertThat(yaml).contains("type: \"TRANSITION\"");
        assertThat(yaml).contains("text: \"切至：走廊\"");
        assertThat(yaml).doesNotContain("visualizedInnerThoughts");
        assertThat(yaml).doesNotContain("她松了口气");
        assertThat(yaml).doesNotContain("sourceText");
        assertThat(yaml).doesNotContain("第一段原文。");
        assertThat(yaml).doesNotContain(": null");
    }

    @Test
    void updatesPersistedSceneAndExportUsesUpdatedSceneJson() {
        ScreenplayConversionEntity conversion = new ScreenplayConversionEntity();
        conversion.setId("cv-1234abcd");
        conversion.setNovelId("nv-1234abcd");
        conversion.setScreenplayType(ScreenplayTypeEnum.ANIME);
        conversion.setStatus("COMPLETED");

        ScreenplaySceneEntity sceneEntity = new ScreenplaySceneEntity();
        sceneEntity.setConversionId("cv-1234abcd");
        sceneEntity.setChapterIndex(1);
        sceneEntity.setSceneIndexInChapter(1);
        sceneEntity.setSceneJson("""
                {
                  "sceneId": "s1",
                  "heading": {"interior": true, "location": "教室", "timeOfDay": "午后"},
                  "actionLines": ["林秋把书包放在桌上。"],
                  "dialogueBlocks": [],
                  "visualizedInnerThoughts": [],
                  "transitions": [],
                  "sourceChapter": 1,
                  "sourceText": "第一段原文。"
                }
                """);
        SceneDTO updatedScene = scene("s1");
        updatedScene.setActionLines(List.of("她抬头看向窗外。"));

        when(sceneRepository.findByConversionIdAndChapterIndexAndSceneIndexInChapter("cv-1234abcd", 1, 1))
                .thenReturn(Optional.of(sceneEntity));
        when(sceneRepository.save(sceneEntity)).thenReturn(sceneEntity);
        when(conversionRepository.findById("cv-1234abcd")).thenReturn(Optional.of(conversion));
        when(sceneRepository.findByConversionIdOrderByChapterIndexAscSceneIndexInChapterAsc("cv-1234abcd"))
                .thenReturn(List.of(sceneEntity));

        SceneDTO savedScene = screenplayService.updatePersistedScene("cv-1234abcd", 1, 1, updatedScene);
        String yaml = screenplayService.exportConversionYaml("cv-1234abcd");

        assertThat(savedScene.getActionLines()).containsExactly("她抬头看向窗外。");
        assertThat(sceneEntity.getSceneJson()).contains("她抬头看向窗外。");
        assertThat(yaml).contains("她抬头看向窗外。");
    }

    @Test
    void retriesWhenLongChapterFallsBackToSingleWholeChapterScene() throws Exception {
        String novelId = "nv-1234abcd";
        String chapterContent = "第一段。\n第二段。\n第三段。\n第四段。\n第五段。\n第六段。";
        SceneUnitDTO degradedUnit = sceneUnit(1, 1, 1, 6);
        SceneUnitDTO firstUnit = sceneUnit(1, 1, 1, 3);
        SceneUnitDTO secondUnit = sceneUnit(1, 2, 4, 6);
        CountDownLatch releaseGetChapters = new CountDownLatch(1);

        when(novelService.getChapters(novelId)).thenAnswer(invocation -> {
            releaseGetChapters.await(2, TimeUnit.SECONDS);
            return chaptersResult(novelId);
        });
        when(novelService.getChapterDetail(novelId, 1)).thenReturn(chapterDetail(novelId, chapterContent));
        when(llmService.splitChapterIntoScenes(chapterContent, 1, ScreenplayTypeEnum.ANIME))
            .thenReturn(List.of(degradedUnit))
            .thenReturn(List.of(firstUnit, secondUnit));
        when(llmService.convertSingleScene("第一段。\n第二段。\n第三段。", ScreenplayTypeEnum.ANIME)).thenReturn(scene("scene-1"));
        when(llmService.convertSingleScene("第四段。\n第五段。\n第六段。", ScreenplayTypeEnum.ANIME)).thenReturn(scene("scene-2"));

        CountDownLatch completion = new CountDownLatch(1);
        SseEmitter emitter = screenplayService.convertNovel(novelId, ScreenplayTypeEnum.ANIME);
        initializeEmitterHandler(emitter, new CopyOnWriteArrayList<>(), completion, new AtomicReference<>());
        releaseGetChapters.countDown();
        assertThat(completion.await(2, TimeUnit.SECONDS)).isTrue();

        verify(llmService, times(2)).splitChapterIntoScenes(chapterContent, 1, ScreenplayTypeEnum.ANIME);
        verify(llmService, times(1)).convertSingleScene("第一段。\n第二段。\n第三段。", ScreenplayTypeEnum.ANIME);
        verify(llmService, times(1)).convertSingleScene("第四段。\n第五段。\n第六段。", ScreenplayTypeEnum.ANIME);
    }

    @Test
    void retriesSingleSceneWhenLanguageDriftIsDetected() throws Exception {
        String novelId = "nv-1234abcd";
        String chapterContent = "第一段。\n第二段。";
        SceneUnitDTO sceneUnit = sceneUnit(1, 1, 1, 2);
        CountDownLatch releaseGetChapters = new CountDownLatch(1);
        List<Object> sentPayloads = new CopyOnWriteArrayList<>();

        when(novelService.getChapters(novelId)).thenAnswer(invocation -> {
            releaseGetChapters.await(2, TimeUnit.SECONDS);
            return chaptersResult(novelId);
        });
        when(novelService.getChapterDetail(novelId, 1)).thenReturn(chapterDetail(novelId, chapterContent));
        when(llmService.splitChapterIntoScenes(chapterContent, 1, ScreenplayTypeEnum.ANIME))
                .thenReturn(List.of(sceneUnit));
        when(llmService.convertSingleScene(chapterContent, ScreenplayTypeEnum.ANIME))
                .thenThrow(new SceneLanguageDriftException("scriptBlocks[0].line", "もう大丈夫。"))
                .thenReturn(scene("scene-after-retry"));

        CountDownLatch completion = new CountDownLatch(1);
        SseEmitter emitter = screenplayService.convertNovel(novelId, ScreenplayTypeEnum.ANIME);
        initializeEmitterHandler(emitter, sentPayloads, completion, new AtomicReference<>());
        releaseGetChapters.countDown();
        assertThat(completion.await(2, TimeUnit.SECONDS)).isTrue();

        verify(llmService, times(2)).convertSingleScene(chapterContent, ScreenplayTypeEnum.ANIME);
        assertThat(containsEvent(sentPayloads, "failed")).isFalse();
        assertThat(containsEvent(sentPayloads, "completed")).isTrue();
        assertThat(containsFreshScene(sentPayloads, 1)).isTrue();
    }

    @Test
    void acceptsLastSceneWhenLanguageDriftRetriesAreExhausted() throws Exception {
        String novelId = "nv-1234abcd";
        String chapterContent = "第一段。\n第二段。";
        SceneUnitDTO sceneUnit = sceneUnit(1, 1, 1, 2);
        SceneDTO driftScene = scene("scene-with-drift");
        CountDownLatch releaseGetChapters = new CountDownLatch(1);
        List<Object> sentPayloads = new CopyOnWriteArrayList<>();

        when(novelService.getChapters(novelId)).thenAnswer(invocation -> {
            releaseGetChapters.await(2, TimeUnit.SECONDS);
            return chaptersResult(novelId);
        });
        when(novelService.getChapterDetail(novelId, 1)).thenReturn(chapterDetail(novelId, chapterContent));
        when(llmService.splitChapterIntoScenes(chapterContent, 1, ScreenplayTypeEnum.ANIME))
                .thenReturn(List.of(sceneUnit));
        when(llmService.convertSingleScene(chapterContent, ScreenplayTypeEnum.ANIME))
                .thenThrow(new SceneLanguageDriftException("scriptBlocks[0].text", "雨森たきび", driftScene))
                .thenThrow(new SceneLanguageDriftException("scriptBlocks[0].text", "雨森たきび", driftScene))
                .thenThrow(new SceneLanguageDriftException("scriptBlocks[0].text", "雨森たきび", driftScene));

        CountDownLatch completion = new CountDownLatch(1);
        SseEmitter emitter = screenplayService.convertNovel(novelId, ScreenplayTypeEnum.ANIME);
        initializeEmitterHandler(emitter, sentPayloads, completion, new AtomicReference<>());
        releaseGetChapters.countDown();
        assertThat(completion.await(2, TimeUnit.SECONDS)).isTrue();

        verify(llmService, times(3)).convertSingleScene(chapterContent, ScreenplayTypeEnum.ANIME);
        verify(sceneRepository, times(1)).save(org.mockito.ArgumentMatchers.any(ScreenplaySceneEntity.class));
        assertThat(containsEvent(sentPayloads, "failed")).isFalse();
        assertThat(containsEvent(sentPayloads, "completed")).isTrue();
        assertThat(containsMessage(sentPayloads, "该场生成结果可能含有非中文表达，请在预览或打磨时重点检查。")).isTrue();
    }

    @Test
    void emitsFailedEventWhenSceneAnchorsCannotBeMatched() throws Exception {
        String novelId = "nv-1234abcd";
        String chapterContent = "第一段原文。\n第二段原文。\n第三段原文。";
        SceneUnitDTO invalidUnit = sceneUnit(1, 1, 99, 100);
        CountDownLatch releaseGetChapters = new CountDownLatch(1);
        AtomicReference<Throwable> completionError = new AtomicReference<>();
        List<Object> sentPayloads = new CopyOnWriteArrayList<>();

        when(novelService.getChapters(novelId)).thenAnswer(invocation -> {
            releaseGetChapters.await(2, TimeUnit.SECONDS);
            return chaptersResult(novelId);
        });
        when(novelService.getChapterDetail(novelId, 1)).thenReturn(chapterDetail(novelId, chapterContent));
        when(llmService.splitChapterIntoScenes(chapterContent, 1, ScreenplayTypeEnum.ANIME))
            .thenReturn(List.of(invalidUnit));

        CountDownLatch completion = new CountDownLatch(1);
        SseEmitter emitter = screenplayService.convertNovel(novelId, ScreenplayTypeEnum.ANIME);
        initializeEmitterHandler(emitter, sentPayloads, completion, completionError);
        releaseGetChapters.countDown();
        assertThat(completion.await(2, TimeUnit.SECONDS)).isTrue();

        assertThat(completionError.get()).isNull();
        assertThat(containsEvent(sentPayloads, "failed")).isTrue();
        assertThat(containsReason(sentPayloads, "chapterIndex=1")).isTrue();
        assertThat(containsReason(sentPayloads, "sceneIndexInChapter=1")).isTrue();
        assertThat(containsReason(sentPayloads, "endSegmentIndex")).isTrue();
        verify(llmService, never()).convertSingleScene(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(ScreenplayTypeEnum.ANIME));
    }

    @Test
    void emitsFailedEventWhenSceneUnitsLeaveSegmentGapAfterRetry() throws Exception {
        String novelId = "nv-1234abcd";
        String chapterContent = "第一段。\n第二段。\n第三段。";
        SceneUnitDTO firstUnit = sceneUnit(1, 1, 1, 1);
        SceneUnitDTO secondUnit = sceneUnit(1, 2, 3, 3);
        CountDownLatch releaseGetChapters = new CountDownLatch(1);
        AtomicReference<Throwable> completionError = new AtomicReference<>();
        List<Object> sentPayloads = new CopyOnWriteArrayList<>();

        when(novelService.getChapters(novelId)).thenAnswer(invocation -> {
            releaseGetChapters.await(2, TimeUnit.SECONDS);
            return chaptersResult(novelId);
        });
        when(novelService.getChapterDetail(novelId, 1)).thenReturn(chapterDetail(novelId, chapterContent));
        when(llmService.splitChapterIntoScenes(chapterContent, 1, ScreenplayTypeEnum.ANIME))
            .thenReturn(List.of(firstUnit, secondUnit));

        CountDownLatch completion = new CountDownLatch(1);
        SseEmitter emitter = screenplayService.convertNovel(novelId, ScreenplayTypeEnum.ANIME);
        initializeEmitterHandler(emitter, sentPayloads, completion, completionError);
        releaseGetChapters.countDown();
        assertThat(completion.await(2, TimeUnit.SECONDS)).isTrue();

        assertThat(completionError.get()).isNull();
        assertThat(containsEvent(sentPayloads, "failed")).isTrue();
        assertThat(containsReason(sentPayloads, "片段不连续")).isTrue();
        verify(llmService, times(2)).splitChapterIntoScenes(chapterContent, 1, ScreenplayTypeEnum.ANIME);
        verify(llmService, never()).convertSingleScene(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(ScreenplayTypeEnum.ANIME)
        );
    }

    @Test
    void emitsFailedEventWhenSceneSplitValidationFails() throws Exception {
        String novelId = "nv-1234abcd";
        String chapterContent = "第一段。\n第二段。\n第三段。";
        SceneUnitDTO firstUnit = sceneUnit(1, 1, 1, 1);
        SceneUnitDTO secondUnit = sceneUnit(1, 2, 3, 3);
        CountDownLatch releaseGetChapters = new CountDownLatch(1);
        AtomicReference<Throwable> completionError = new AtomicReference<>();
        List<Object> sentPayloads = new CopyOnWriteArrayList<>();

        when(novelService.getChapters(novelId)).thenAnswer(invocation -> {
            releaseGetChapters.await(2, TimeUnit.SECONDS);
            return chaptersResult(novelId);
        });
        when(novelService.getChapterDetail(novelId, 1)).thenReturn(chapterDetail(novelId, chapterContent));
        when(llmService.splitChapterIntoScenes(chapterContent, 1, ScreenplayTypeEnum.ANIME))
            .thenReturn(List.of(firstUnit, secondUnit));

        CountDownLatch completion = new CountDownLatch(1);
        SseEmitter emitter = screenplayService.convertNovel(novelId, ScreenplayTypeEnum.ANIME);
        initializeEmitterHandler(emitter, sentPayloads, completion, completionError);
        releaseGetChapters.countDown();
        assertThat(completion.await(2, TimeUnit.SECONDS)).isTrue();

        assertThat(completionError.get()).isNull();
        assertThat(containsEvent(sentPayloads, "failed")).isTrue();
        assertThat(containsMessage(sentPayloads, "转换中断，可继续转换；系统会跳过已完成部分。")).isTrue();
        verify(llmService, never()).convertSingleScene(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(ScreenplayTypeEnum.ANIME)
        );
    }

    private void initializeEmitterHandler(
        SseEmitter emitter,
        List<Object> sentPayloads,
        CountDownLatch completion,
        AtomicReference<Throwable> completionError
    )
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        Class<?> handlerType = Class.forName(
            "org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter$Handler"
        );
        Object handler = Proxy.newProxyInstance(
            handlerType.getClassLoader(),
            new Class<?>[]{handlerType},
            (proxy, method, args) -> {
                if ("send".equals(method.getName()) && args != null && args.length > 0) {
                    sentPayloads.add(args[0]);
                }
                if ("completeWithError".equals(method.getName()) && args != null && args.length > 0) {
                    completionError.set((Throwable) args[0]);
                }
                if ("complete".equals(method.getName()) || "completeWithError".equals(method.getName())) {
                    completion.countDown();
                }
                return null;
            }
        );
        Method initializeMethod = ResponseBodyEmitter.class.getDeclaredMethod("initialize", handlerType);
        initializeMethod.setAccessible(true);
        initializeMethod.invoke(emitter, handler);
    }

    private boolean containsChapterSplitEvent(List<Object> sentPayloads) {
        return containsEvent(sentPayloads, "chapter_split");
    }

    private boolean containsEvent(List<Object> sentPayloads, String eventName) {
        return flattenPayloadData(sentPayloads).stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .anyMatch(data -> data.contains("event:" + eventName));
    }

    private boolean containsReplayedEvent(List<Object> sentPayloads, String eventName) {
        return flattenPayloadData(sentPayloads).stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .anyMatch(data -> eventName.equals(data.get("eventName"))
                && Boolean.TRUE.equals(data.get("replayed")));
    }

    private boolean containsFreshScene(List<Object> sentPayloads, int sceneIndexInChapter) {
        return flattenPayloadData(sentPayloads).stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .anyMatch(data -> "scene_completed".equals(data.get("eventName"))
                && !Boolean.TRUE.equals(data.get("replayed"))
                && data.get("sceneIndexInChapter") instanceof Integer index
                && index == sceneIndexInChapter);
    }

    private boolean containsMessage(List<Object> sentPayloads, String expectedMessage) {
        return flattenPayloadData(sentPayloads).stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .anyMatch(data -> expectedMessage.equals(data.get("message")));
    }

    private boolean containsReason(List<Object> sentPayloads, String expectedReasonPart) {
        return flattenPayloadData(sentPayloads).stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .anyMatch(data -> data.get("reason") instanceof String reason
                && reason.contains(expectedReasonPart));
    }

    private boolean containsConversionId(List<Object> sentPayloads) {
        return flattenPayloadData(sentPayloads).stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .anyMatch(data -> data.get("conversionId") instanceof String conversionId
                && conversionId.startsWith("cv-"));
    }

    private boolean containsSceneCount(List<Object> sentPayloads, int expectedSceneCount) {
        return flattenPayloadData(sentPayloads).stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .anyMatch(data -> data.containsKey("sceneCount")
                && data.get("sceneCount") instanceof Integer sceneCount
                && expectedSceneCount == sceneCount);
    }

    private List<Object> flattenPayloadData(List<Object> sentPayloads) {
        List<Object> flattened = new ArrayList<>();
        for (Object payload : sentPayloads) {
            if (payload instanceof Set<?> batch) {
                for (Object item : batch) {
                    if (item instanceof ResponseBodyEmitter.DataWithMediaType dataWithMediaType) {
                        flattened.add(dataWithMediaType.getData());
                    }
                }
            } else {
                flattened.add(payload);
            }
        }
        return flattened;
    }

    private NovelChaptersResultDTO chaptersResult(String novelId) {
        ChapterPreviewDTO chapter = new ChapterPreviewDTO();
        chapter.setChapterIndex(1);
        chapter.setTitle("第一章");

        NovelChaptersResultDTO result = new NovelChaptersResultDTO();
        result.setNovelId(novelId);
        result.setTitle("测试小说");
        result.setTotalChapters(1);
        result.setChapters(List.of(chapter));
        return result;
    }

    private NovelChapterDetailDTO chapterDetail(String novelId, String content) {
        NovelChapterDetailDTO detail = new NovelChapterDetailDTO();
        detail.setNovelId(novelId);
        detail.setChapterIndex(1);
        detail.setTitle("第一章");
        detail.setContent(content);
        return detail;
    }

    private SceneUnitDTO sceneUnit(int chapterIndex, int sceneIndexInChapter, int startSegmentIndex, int endSegmentIndex) {
        SceneUnitDTO dto = new SceneUnitDTO();
        dto.setSourceChapter(chapterIndex);
        dto.setSceneIndexInChapter(sceneIndexInChapter);
        dto.setTitle("切分场景");
        dto.setSummary("切分摘要");
        dto.setStartSegmentIndex(startSegmentIndex);
        dto.setEndSegmentIndex(endSegmentIndex);
        return dto;
    }

    private SceneDTO scene(String sceneId) {
        SceneDTO scene = new SceneDTO();
        scene.setSceneId(sceneId);
        scene.setSourceText("测试场景原文");
        scene.setSourceChapter(1);
        return scene;
    }

    private ScreenplayConversionEntity conversion(String conversionId, String novelId, String status) {
        ScreenplayConversionEntity conversion = new ScreenplayConversionEntity();
        conversion.setId(conversionId);
        conversion.setNovelId(novelId);
        conversion.setScreenplayType(ScreenplayTypeEnum.ANIME);
        conversion.setStatus(status);
        return conversion;
    }

    private ScreenplaySceneUnitEntity sceneUnitEntity(
        String conversionId,
        int chapterIndex,
        int sceneIndexInChapter,
        String title,
        String sourceText
    ) {
        ScreenplaySceneUnitEntity entity = new ScreenplaySceneUnitEntity();
        entity.setConversionId(conversionId);
        entity.setChapterIndex(chapterIndex);
        entity.setSceneIndexInChapter(sceneIndexInChapter);
        entity.setTitle(title);
        entity.setSummary("历史切场摘要");
        entity.setStartSegmentIndex(sceneIndexInChapter);
        entity.setEndSegmentIndex(sceneIndexInChapter);
        entity.setSourceText(sourceText);
        entity.setStatus("SPLIT");
        return entity;
    }

    private ScreenplaySceneEntity sceneEntity(String conversionId, int chapterIndex, int sceneIndexInChapter, String sceneId) {
        ScreenplaySceneEntity entity = new ScreenplaySceneEntity();
        entity.setConversionId(conversionId);
        entity.setChapterIndex(chapterIndex);
        entity.setSceneIndexInChapter(sceneIndexInChapter);
        entity.setSceneJson("""
                {
                  "sceneId": "%s",
                  "heading": {"interior": true, "location": "教室", "timeOfDay": "午后"},
                  "actionLines": ["林秋把书包放在桌上。"],
                  "dialogueBlocks": [],
                  "visualizedInnerThoughts": [],
                  "transitions": [],
                  "sourceChapter": %d,
                  "sourceText": "第一段原文。"
                }
                """.formatted(sceneId, chapterIndex));
        return entity;
    }
}
