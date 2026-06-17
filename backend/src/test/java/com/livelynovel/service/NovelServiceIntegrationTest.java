package com.livelynovel.service;

import com.livelynovel.model.dto.NovelChaptersResultDTO;
import com.livelynovel.model.dto.NovelListItemDTO;
import com.livelynovel.model.dto.NovelUploadResultDTO;
import com.livelynovel.model.entity.ScreenplayConversionEntity;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.repository.ScreenplayConversionRepository;
import com.livelynovel.repository.NovelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 小说持久化链路集成测试：真实走 JPA + SQLite，不连 LLM。
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/novel-service-integration.db",
        "spring.jpa.hibernate.ddl-auto=update"
})
class NovelServiceIntegrationTest {

    private static final String SAMPLE = """
            ==========
            ~一章~
            ==========

            第一章正文第一句。

            ==========
            ~二章~
            ==========

            第二章正文第二句。

            ==========
            ~三章~
            ==========

            第三章正文第三句。
            """;

    @Autowired
    private NovelService novelService;

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private ScreenplayConversionRepository screenplayConversionRepository;

    @BeforeEach
    void clearRepository() {
        screenplayConversionRepository.deleteAll();
        novelRepository.deleteAll();
    }

    @Test
    void uploadPersistsNovelAndGetChaptersReturnsPreview() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                SAMPLE.getBytes(StandardCharsets.UTF_8)
        );

        NovelUploadResultDTO uploadResult = novelService.uploadTxt("测试上传", file);

        assertThat(uploadResult.getNovelId()).startsWith("nv-");
        assertThat(uploadResult.getContentHash()).startsWith("sha256:");
        assertThat(novelRepository.findById(uploadResult.getNovelId())).isPresent();

        NovelChaptersResultDTO chaptersResult = novelService.getChapters(uploadResult.getNovelId());

        assertThat(chaptersResult.getTitle()).isEqualTo("测试上传");
        assertThat(chaptersResult.getTotalChapters()).isEqualTo(3);
        assertThat(chaptersResult.getChapters()).hasSize(3);
        assertThat(chaptersResult.getChapters().get(0).getPreview()).contains("第一章正文");
    }

    @Test
    void uploadUsesFilenameAsDefaultTitleAndCanUpdateTitle() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "默认标题.txt",
                "text/plain",
                SAMPLE.getBytes(StandardCharsets.UTF_8)
        );

        NovelUploadResultDTO uploadResult = novelService.uploadTxt("", file);

        assertThat(uploadResult.getTitle()).isEqualTo("默认标题");

        NovelChaptersResultDTO updatedResult = novelService.updateTitle(uploadResult.getNovelId(), "新标题");

        assertThat(updatedResult.getTitle()).isEqualTo("新标题");
        assertThat(novelRepository.findById(uploadResult.getNovelId()).orElseThrow().getTitle()).isEqualTo("新标题");
        assertThat(novelService.listNovels().getTotal()).isEqualTo(1);
        assertThat(novelRepository.count()).isEqualTo(1);
    }

    @Test
    void listNovelsIncludesLatestConversionSummary() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "转换状态.txt",
                "text/plain",
                SAMPLE.getBytes(StandardCharsets.UTF_8)
        );

        NovelUploadResultDTO uploadResult = novelService.uploadTxt("转换状态小说", file);

        ScreenplayConversionEntity running = new ScreenplayConversionEntity();
        running.setId("cv-running");
        running.setNovelId(uploadResult.getNovelId());
        running.setScreenplayType(ScreenplayTypeEnum.ANIME);
        running.setStatus("RUNNING");
        running.setErrorMessage(null);
        running.setCreatedAt(Instant.parse("2026-06-17T01:00:00Z"));
        running.setUpdatedAt(Instant.parse("2026-06-17T01:10:00Z"));
        screenplayConversionRepository.save(running);

        ScreenplayConversionEntity failed = new ScreenplayConversionEntity();
        failed.setId("cv-failed");
        failed.setNovelId(uploadResult.getNovelId());
        failed.setScreenplayType(ScreenplayTypeEnum.ANIME);
        failed.setStatus("FAILED");
        failed.setErrorMessage("测试失败");
        failed.setCreatedAt(Instant.parse("2026-06-17T02:00:00Z"));
        failed.setUpdatedAt(Instant.parse("2026-06-17T02:10:00Z"));
        screenplayConversionRepository.save(failed);

        NovelListItemDTO item = novelService.listNovels().getNovels().get(0);

        assertThat(item.getNovelId()).isEqualTo(uploadResult.getNovelId());
        assertThat(item.getLatestConversionStatus()).isEqualTo("FAILED");
        assertThat(item.getLatestConversionId()).isEqualTo("cv-failed");
        assertThat(item.getLatestConversionType()).isEqualTo("ANIME");
        assertThat(item.getLatestConversionUpdatedAt()).isEqualTo("2026-06-17T02:10:00Z");
        assertThat(item.getLatestConversionErrorMessage()).isEqualTo("测试失败");
    }
}
