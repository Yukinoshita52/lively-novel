package com.livelynovel.service;

import com.livelynovel.model.dto.ChapterDTO;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 真实全本验证（可选、本地运行）。
 *
 * <p>读取 {@code temp/败犬女主太多了.txt} 验证切分效果。该文件为有版权的轻小说，
 * <b>不提交进仓库</b>（{@code temp/} 已被 gitignore）；文件不存在时本测试自动跳过，
 * 不影响 CI / 他人构建。
 */
class ChapterSplitterRealFileTest {

    /** 相对 backend 模块目录（Maven 测试工作目录）→ 项目根的 temp。 */
    private static final Path NOVEL_PATH = Path.of("..", "temp", "败犬女主太多了.txt");

    private final ChapterSplitter splitter = new ChapterSplitter();

    @Test
    void splitsRealLightNovelIntoFourMainChapters() throws IOException {
        assumeTrue(Files.exists(NOVEL_PATH),
                "本地小说文件不存在，跳过：" + NOVEL_PATH.toAbsolutePath());

        String text = Files.readString(NOVEL_PATH);
        List<ChapterDTO> chapters = splitter.split(text);

        // 恰好 4 个主章节（一/二/三/四败目），Intermission / 后记 / 简介均不算章节
        assertThat(chapters).hasSize(4);
        assertThat(chapters).extracting(ChapterDTO::getChapterIndex)
                .containsExactly(1, 2, 3, 4);
        assertThat(chapters).allSatisfy(c -> {
            assertThat(c.getTitle()).contains("败目");
            assertThat(c.getContent()).isNotBlank();
            assertThat(c.getWordCount()).isGreaterThan(0);
        });
        // 前置冗余（简介）不应进入任何章节
        assertThat(chapters).noneSatisfy(c ->
                assertThat(c.getContent()).contains("败犬们交织的谜之青春就此开幕"));
    }
}
