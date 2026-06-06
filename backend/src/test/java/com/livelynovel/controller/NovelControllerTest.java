package com.livelynovel.controller;

import com.livelynovel.common.Result;
import com.livelynovel.model.dto.ChapterPreviewDTO;
import com.livelynovel.model.dto.NovelChapterDetailDTO;
import com.livelynovel.model.dto.NovelChaptersResultDTO;
import com.livelynovel.model.dto.NovelListItemDTO;
import com.livelynovel.model.dto.NovelListResultDTO;
import com.livelynovel.model.dto.NovelParseRequestDTO;
import com.livelynovel.model.dto.NovelParseResultDTO;
import com.livelynovel.model.dto.NovelUploadResultDTO;
import com.livelynovel.service.ChapterSplitter;
import com.livelynovel.service.NovelService;
import com.livelynovel.service.impl.ChapterSplitterImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@code POST /api/novel/parse} 接口测试（纯单元测试，不起 Spring、不连 LLM）。
 */
class NovelControllerTest {

    private static final String SAMPLE = """
            ==========
            ~一章~
            ==========

            第一章正文。

            ==========
            ~二章~
            ==========

            第二章正文。

            ==========
            ~三章~
            ==========

            第三章正文。
            """;

    private final NovelService novelService = mock(NovelService.class);
    private final ChapterSplitter chapterSplitter = new ChapterSplitterImpl();
    private final NovelController controller = new NovelController(chapterSplitter, novelService);

    private NovelParseRequestDTO request(String title, String text) {
        NovelParseRequestDTO req = new NovelParseRequestDTO();
        req.setTitle(title);
        req.setText(text);
        return req;
    }

    @Test
    void parsesValidNovelIntoChapters() {
        Result<NovelParseResultDTO> result = controller.parse(request("测试小说", SAMPLE));

        assertThat(result.getCode()).isEqualTo(0);
        NovelParseResultDTO data = result.getData();
        assertThat(data.getTitle()).isEqualTo("测试小说");
        assertThat(data.getTotalChapters()).isEqualTo(3);
        assertThat(data.getChapters()).extracting("title")
                .containsExactly("一章", "二章", "三章");
        // totalWordCount = 各章字数之和
        int sum = data.getChapters().stream().mapToInt(c -> c.getWordCount()).sum();
        assertThat(data.getTotalWordCount()).isEqualTo(sum);
    }

    @Test
    void rejectsEmptyTextWith40001() {
        assertThat(controller.parse(request("t", "")).getCode()).isEqualTo(40001);
        assertThat(controller.parse(request("t", "   ")).getCode()).isEqualTo(40001);
    }

    @Test
    void rejectsOversizedTextWith40002() {
        String huge = "字".repeat(200_001);
        assertThat(controller.parse(request("t", huge)).getCode()).isEqualTo(40002);
    }

    @Test
    void rejectsFewerThanThreeChaptersWith40003() {
        String twoChapters = """
                ==========
                ~一章~
                ==========

                正文一。

                ==========
                ~二章~
                ==========

                正文二。
                """;
        assertThat(controller.parse(request("t", twoChapters)).getCode()).isEqualTo(40003);
    }

    @Test
    void uploadsTxtNovelAndReturnsNovelIdAndContentHash() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                SAMPLE.getBytes(StandardCharsets.UTF_8)
        );
        NovelUploadResultDTO uploadResult = new NovelUploadResultDTO();
        uploadResult.setNovelId("nv-1234abcd");
        uploadResult.setTitle("上传小说");
        uploadResult.setContentHash("sha256:abc123");
        uploadResult.setTotalChapters(3);
        uploadResult.setTotalWordCount(12);

        when(novelService.uploadTxt("上传小说", file)).thenReturn(uploadResult);

        Result<NovelUploadResultDTO> result = controller.upload("上传小说", file);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData().getNovelId()).isEqualTo("nv-1234abcd");
        assertThat(result.getData().getContentHash()).isEqualTo("sha256:abc123");
        assertThat(result.getData().getTotalChapters()).isEqualTo(3);
    }

    @Test
    void rejectsEmptyUploadFileWith40001() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        Result<NovelUploadResultDTO> result = controller.upload("空文件", empty);

        assertThat(result.getCode()).isEqualTo(40001);
        verifyNoInteractions(novelService);
    }

    @Test
    void returnsStoredNovelChaptersWithPreview() {
        NovelChaptersResultDTO resultData = new NovelChaptersResultDTO();
        resultData.setNovelId("nv-1234abcd");
        resultData.setTitle("已存小说");
        resultData.setTotalChapters(3);
        resultData.setTotalWordCount(12);

        ChapterPreviewDTO chapter = new ChapterPreviewDTO();
        chapter.setChapterIndex(1);
        chapter.setTitle("一章");
        chapter.setWordCount(4);
        chapter.setPreview("第一章正文……");
        resultData.setChapters(java.util.List.of(chapter));

        when(novelService.getChapters("nv-1234abcd")).thenReturn(resultData);

        Result<NovelChaptersResultDTO> result = controller.getChapters("nv-1234abcd");

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData().getChapters()).hasSize(1);
        assertThat(result.getData().getChapters().get(0).getPreview()).isEqualTo("第一章正文……");
    }

    @Test
    void returns40401WhenNovelDoesNotExist() {
        when(novelService.getChapters("nv-missing")).thenReturn(null);

        Result<NovelChaptersResultDTO> result = controller.getChapters("nv-missing");

        assertThat(result.getCode()).isEqualTo(40401);
    }

    @Test
    void returnsStoredNovelList() {
        NovelListItemDTO item = new NovelListItemDTO();
        item.setNovelId("nv-1234abcd");
        item.setTitle("已存小说");
        item.setTotalChapters(3);
        item.setTotalWordCount(12);
        item.setCreatedAt("2026-06-06T08:00:00Z");

        NovelListResultDTO resultData = new NovelListResultDTO();
        resultData.setNovels(java.util.List.of(item));
        resultData.setTotal(1);

        when(novelService.listNovels()).thenReturn(resultData);

        Result<NovelListResultDTO> result = controller.listNovels();

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData().getTotal()).isEqualTo(1);
        assertThat(result.getData().getNovels()).hasSize(1);
        assertThat(result.getData().getNovels().get(0).getTitle()).isEqualTo("已存小说");
    }

    @Test
    void returnsEmptyNovelList() {
        NovelListResultDTO resultData = new NovelListResultDTO();
        resultData.setNovels(java.util.List.of());
        resultData.setTotal(0);

        when(novelService.listNovels()).thenReturn(resultData);

        Result<NovelListResultDTO> result = controller.listNovels();

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData().getTotal()).isEqualTo(0);
        assertThat(result.getData().getNovels()).isEmpty();
    }

    @Test
    void returnsStoredNovelChapterDetail() {
        NovelChapterDetailDTO detail = new NovelChapterDetailDTO();
        detail.setNovelId("nv-1234abcd");
        detail.setChapterIndex(2);
        detail.setTitle("第二章 天台");
        detail.setContent("黄昏，城市在脚下铺开。");
        detail.setWordCount(12);

        when(novelService.getChapterDetail("nv-1234abcd", 2)).thenReturn(detail);

        Result<NovelChapterDetailDTO> result = controller.getChapterDetail("nv-1234abcd", 2);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData().getChapterIndex()).isEqualTo(2);
        assertThat(result.getData().getContent()).contains("黄昏");
    }

    @Test
    void returns40401WhenChapterDetailDoesNotExist() {
        when(novelService.getChapterDetail("nv-missing", 1)).thenReturn(null);

        Result<NovelChapterDetailDTO> result = controller.getChapterDetail("nv-missing", 1);

        assertThat(result.getCode()).isEqualTo(40401);
    }
}
