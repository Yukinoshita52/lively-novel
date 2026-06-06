package com.livelynovel.service;

import com.livelynovel.model.dto.ChapterDTO;
import com.livelynovel.service.impl.ChapterSplitterImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 章节切分器测试（纯单元测试，不依赖 Spring / 不连 LLM）。
 *
 * <p>用合成的 {@code ===} 围栏 / {@code ~X~} 样本验证切分逻辑，
 * 不引入版权小说内容。真实全本的验证见 {@code ChapterSplitterRealFileTest}。
 */
class ChapterSplitterTest {

    /**
     * 合成样本，模拟日式轻小说的 {@code ===} 围栏结构：
     * <ul>
     *   <li>前置冗余块（书名 / 简介）——标题非波浪号包围，应被排除</li>
     *   <li>3 个 {@code ~X章~} 主章节——应被识别</li>
     *   <li>夹在第一、二章之间的 Intermission——标题非波浪号包围，应折入第一章正文</li>
     * </ul>
     */
    private static final String SAMPLE = """
            ==========
            合成小说标题
            ==========
            作者: 测试

            ==========
            简介
            ==========

            这是一段简介，不应被识别为章节。

            ==========
            ~一章~
            青梅竹马
            ==========

            第一章的第一句话。
            第一章的第二句话。

            ==========
            间章Intermission
            夜晚的厨房
            ==========

            间章内容，应折入第一章。

            ==========
            ~二章~
            契约
            ==========

            第二章正文。

            ==========
            ~三章~
            ==========

            第三章正文。
            """;

    private final ChapterSplitter splitter = new ChapterSplitterImpl();

    @Test
    void splitsOnlyTildeFencedMainChapters() {
        List<ChapterDTO> chapters = splitter.split(SAMPLE);

        assertThat(chapters).hasSize(3);
        assertThat(chapters).extracting(ChapterDTO::getChapterIndex)
                .containsExactly(1, 2, 3);
        assertThat(chapters).extracting(ChapterDTO::getTitle)
                .containsExactly("一章 青梅竹马", "二章 契约", "三章");
    }

    @Test
    void firstChapterAbsorbsTrailingIntermission() {
        ChapterDTO first = splitter.split(SAMPLE).get(0);

        assertThat(first.getContent())
                .contains("第一章的第一句话。")
                .contains("间章内容，应折入第一章。")   // Intermission 折入上一章
                .doesNotContain("第二章正文");          // 不越界到下一章
    }

    @Test
    void chapterContentIsTrimmedAndWordCounted() {
        ChapterDTO third = splitter.split(SAMPLE).get(2);

        assertThat(third.getContent()).isEqualTo("第三章正文。");
        assertThat(third.getWordCount()).isEqualTo("第三章正文。".length());
    }

    @Test
    void frontMatterIsExcluded() {
        List<ChapterDTO> chapters = splitter.split(SAMPLE);

        assertThat(chapters).noneSatisfy(c ->
                assertThat(c.getContent()).contains("这是一段简介"));
    }

    @Test
    void rejectsBlankText() {
        assertThatThrownBy(() -> splitter.split(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> splitter.split("   \n  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> splitter.split(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
