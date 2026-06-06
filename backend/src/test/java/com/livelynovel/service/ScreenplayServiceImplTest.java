package com.livelynovel.service;

import com.livelynovel.model.dto.ChapterPreviewDTO;
import com.livelynovel.model.dto.NovelChapterDetailDTO;
import com.livelynovel.model.dto.NovelChaptersResultDTO;
import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.dto.SceneUnitDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.service.impl.ChapterSegmentationServiceImpl;
import com.livelynovel.service.impl.ScreenplayServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final ChapterSegmentationService chapterSegmentationService = new ChapterSegmentationServiceImpl();
    private final ScreenplayServiceImpl screenplayService = new ScreenplayServiceImpl(
            novelService,
            llmService,
            chapterSegmentationService
    );

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
    void completesWithErrorWhenSceneAnchorsCannotBeMatched() throws Exception {
        String novelId = "nv-1234abcd";
        String chapterContent = "第一段原文。\n第二段原文。\n第三段原文。";
        SceneUnitDTO invalidUnit = sceneUnit(1, 1, 99, 100);
        CountDownLatch releaseGetChapters = new CountDownLatch(1);
        AtomicReference<Throwable> completionError = new AtomicReference<>();

        when(novelService.getChapters(novelId)).thenAnswer(invocation -> {
            releaseGetChapters.await(2, TimeUnit.SECONDS);
            return chaptersResult(novelId);
        });
        when(novelService.getChapterDetail(novelId, 1)).thenReturn(chapterDetail(novelId, chapterContent));
        when(llmService.splitChapterIntoScenes(chapterContent, 1, ScreenplayTypeEnum.ANIME))
            .thenReturn(List.of(invalidUnit));

        CountDownLatch completion = new CountDownLatch(1);
        SseEmitter emitter = screenplayService.convertNovel(novelId, ScreenplayTypeEnum.ANIME);
        initializeEmitterHandler(emitter, new CopyOnWriteArrayList<>(), completion, completionError);
        releaseGetChapters.countDown();
        assertThat(completion.await(2, TimeUnit.SECONDS)).isTrue();

        assertThat(completionError.get()).isNotNull();
        assertThat(completionError.get())
                .hasMessageContaining("chapterIndex=1")
                .hasMessageContaining("sceneIndexInChapter=1")
                .hasMessageContaining("endSegmentIndex");
        verify(llmService, never()).convertSingleScene(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(ScreenplayTypeEnum.ANIME));
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
        return flattenPayloadData(sentPayloads).stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .anyMatch(data -> data.contains("event:chapter_split"));
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
}
