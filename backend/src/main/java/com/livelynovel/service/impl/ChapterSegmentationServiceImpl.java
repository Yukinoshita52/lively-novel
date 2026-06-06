package com.livelynovel.service.impl;

import com.livelynovel.model.dto.ChapterSegmentDTO;
import com.livelynovel.service.ChapterSegmentationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 章节片段切分实现。
 */
@Service
public class ChapterSegmentationServiceImpl implements ChapterSegmentationService {

    @Override
    public List<ChapterSegmentDTO> segment(String chapterText) {
        if (chapterText == null || chapterText.isBlank()) {
            return List.of();
        }

        String[] blocks = chapterText.strip().split("\\r?\\n");
        List<ChapterSegmentDTO> segments = new ArrayList<>();
        int segmentIndex = 1;
        for (String block : blocks) {
            String text = block == null ? "" : block.strip();
            if (text.isBlank()) {
                continue;
            }
            ChapterSegmentDTO segment = new ChapterSegmentDTO();
            segment.setSegmentIndex(segmentIndex++);
            segment.setText(text);
            segments.add(segment);
        }
        return segments;
    }
}
