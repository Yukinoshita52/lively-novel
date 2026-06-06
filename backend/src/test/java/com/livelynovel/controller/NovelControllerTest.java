package com.livelynovel.controller;

import com.livelynovel.common.Result;
import com.livelynovel.model.dto.NovelParseRequestDTO;
import com.livelynovel.model.dto.NovelParseResultDTO;
import com.livelynovel.service.ChapterSplitter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

    private final NovelController controller = new NovelController(new ChapterSplitter());

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
}
