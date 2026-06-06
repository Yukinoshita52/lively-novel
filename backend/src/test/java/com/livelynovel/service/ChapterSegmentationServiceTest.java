package com.livelynovel.service;

import com.livelynovel.model.dto.ChapterSegmentDTO;
import com.livelynovel.service.impl.ChapterSegmentationServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChapterSegmentationServiceTest {

    private final ChapterSegmentationService service = new ChapterSegmentationServiceImpl();

    @Test
    void splitsChapterIntoOrderedParagraphSegments() {
        String chapterText = """
                第一段原文。

                第二段原文。

                第三段原文。
                """;

        List<ChapterSegmentDTO> segments = service.segment(chapterText);

        assertThat(segments).hasSize(3);
        assertThat(segments.get(0).getSegmentIndex()).isEqualTo(1);
        assertThat(segments.get(0).getText()).isEqualTo("第一段原文。");
        assertThat(segments.get(1).getSegmentIndex()).isEqualTo(2);
        assertThat(segments.get(1).getText()).isEqualTo("第二段原文。");
        assertThat(segments.get(2).getSegmentIndex()).isEqualTo(3);
        assertThat(segments.get(2).getText()).isEqualTo("第三段原文。");
    }

    @Test
    void ignoresBlankParagraphs() {
        String chapterText = """


                第一段原文。



                第二段原文。

                """;

        List<ChapterSegmentDTO> segments = service.segment(chapterText);

        assertThat(segments).hasSize(2);
        assertThat(segments).extracting(ChapterSegmentDTO::getSegmentIndex).containsExactly(1, 2);
    }
}
