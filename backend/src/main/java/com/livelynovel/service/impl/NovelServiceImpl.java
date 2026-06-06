package com.livelynovel.service.impl;

import com.livelynovel.model.entity.NovelEntity;
import com.livelynovel.model.dto.ChapterDTO;
import com.livelynovel.model.dto.ChapterPreviewDTO;
import com.livelynovel.model.dto.NovelChapterDetailDTO;
import com.livelynovel.model.dto.NovelChaptersResultDTO;
import com.livelynovel.model.dto.NovelListItemDTO;
import com.livelynovel.model.dto.NovelListResultDTO;
import com.livelynovel.model.dto.NovelUploadResultDTO;
import com.livelynovel.repository.NovelRepository;
import com.livelynovel.service.ChapterSplitter;
import com.livelynovel.service.NovelService;
import com.livelynovel.common.exception.NovelValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * 小说持久化服务实现。
 */
@Service
public class NovelServiceImpl implements NovelService {

    private static final int MAX_WORD_COUNT = 200_000;
    private static final int MIN_CHAPTERS = 3;
    private static final int PREVIEW_LIMIT = 24;

    private final NovelRepository novelRepository;
    private final ChapterSplitter chapterSplitter;

    public NovelServiceImpl(NovelRepository novelRepository, ChapterSplitter chapterSplitter) {
        this.novelRepository = novelRepository;
        this.chapterSplitter = chapterSplitter;
    }

    @Override
    public NovelUploadResultDTO uploadTxt(String title, MultipartFile file) {
        validateTxtFile(file);

        String rawContent = readUtf8(file);
        if (countWords(rawContent) > MAX_WORD_COUNT) {
            throw new NovelValidationException(40002, "文本超过 " + MAX_WORD_COUNT + " 字上限");
        }
        List<ChapterDTO> chapters = chapterSplitter.split(rawContent);
        if (chapters.size() < MIN_CHAPTERS) {
            throw new NovelValidationException(40003, "章节数不足（需 " + MIN_CHAPTERS + " 章以上），实际识别 "
                    + chapters.size() + " 章");
        }
        String resolvedTitle = resolveTitle(title, file.getOriginalFilename());

        NovelEntity novel = new NovelEntity();
        novel.setId(generateNovelId());
        novel.setTitle(resolvedTitle);
        novel.setContentHash(calculateContentHash(rawContent));
        novel.setTotalChapters(chapters.size());
        novel.setTotalWordCount(chapters.stream().mapToInt(ChapterDTO::getWordCount).sum());
        novel.setRawContent(rawContent);
        novelRepository.save(novel);

        chapters.forEach(chapter -> chapter.setContent(null));

        NovelUploadResultDTO result = new NovelUploadResultDTO();
        result.setNovelId(novel.getId());
        result.setTitle(novel.getTitle());
        result.setContentHash(novel.getContentHash());
        result.setTotalChapters(novel.getTotalChapters());
        result.setTotalWordCount(novel.getTotalWordCount());
        result.setChapters(chapters);
        return result;
    }

    @Override
    public NovelChaptersResultDTO getChapters(String novelId) {
        NovelEntity novel = novelRepository.findById(novelId).orElse(null);
        if (novel == null) {
            return null;
        }

        List<ChapterPreviewDTO> chapterPreviews = chapterSplitter.split(novel.getRawContent()).stream()
                .map(this::toPreview)
                .toList();

        NovelChaptersResultDTO result = new NovelChaptersResultDTO();
        result.setNovelId(novel.getId());
        result.setTitle(novel.getTitle());
        result.setTotalChapters(novel.getTotalChapters());
        result.setTotalWordCount(novel.getTotalWordCount());
        result.setChapters(chapterPreviews);
        return result;
    }

    @Override
    public NovelChapterDetailDTO getChapterDetail(String novelId, int chapterIndex) {
        NovelEntity novel = novelRepository.findById(novelId).orElse(null);
        if (novel == null) {
            return null;
        }

        return chapterSplitter.split(novel.getRawContent()).stream()
                .filter(chapter -> chapter.getChapterIndex() == chapterIndex)
                .findFirst()
                .map(chapter -> toChapterDetail(novel.getId(), chapter))
                .orElse(null);
    }

    @Override
    public NovelListResultDTO listNovels() {
        List<NovelListItemDTO> novels = novelRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(NovelEntity::getCreatedAt).reversed())
                .map(this::toListItem)
                .toList();

        NovelListResultDTO result = new NovelListResultDTO();
        result.setNovels(novels);
        result.setTotal(novels.size());
        return result;
    }

    private void validateTxtFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new NovelValidationException(40001, "文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".txt")) {
            throw new NovelValidationException(40001, "仅支持上传 .txt 文件");
        }
    }

    private String readUtf8(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new NovelValidationException(40001, "读取文件失败");
        }
    }

    private String resolveTitle(String title, String filename) {
        if (title != null && !title.isBlank()) {
            return title.strip();
        }
        if (filename == null || filename.isBlank()) {
            return "未命名小说";
        }

        int dot = filename.lastIndexOf('.');
        return (dot > 0 ? filename.substring(0, dot) : filename).strip();
    }

    private String generateNovelId() {
        return "nv-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String calculateContentHash(String rawContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(rawContent.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private ChapterPreviewDTO toPreview(ChapterDTO chapter) {
        ChapterPreviewDTO preview = new ChapterPreviewDTO();
        preview.setChapterIndex(chapter.getChapterIndex());
        preview.setTitle(chapter.getTitle());
        preview.setWordCount(chapter.getWordCount());
        preview.setPreview(buildPreview(chapter.getContent()));
        return preview;
    }

    private NovelListItemDTO toListItem(NovelEntity novel) {
        NovelListItemDTO item = new NovelListItemDTO();
        item.setNovelId(novel.getId());
        item.setTitle(novel.getTitle());
        item.setTotalChapters(novel.getTotalChapters());
        item.setTotalWordCount(novel.getTotalWordCount());
        item.setCreatedAt(novel.getCreatedAt() == null ? null : novel.getCreatedAt().toString());
        return item;
    }

    private NovelChapterDetailDTO toChapterDetail(String novelId, ChapterDTO chapter) {
        NovelChapterDetailDTO detail = new NovelChapterDetailDTO();
        detail.setNovelId(novelId);
        detail.setChapterIndex(chapter.getChapterIndex());
        detail.setTitle(chapter.getTitle());
        detail.setContent(chapter.getContent());
        detail.setWordCount(chapter.getWordCount());
        return detail;
    }

    private String buildPreview(String content) {
        String normalized = content == null ? "" : content.replaceAll("\\s+", " ").strip();
        if (normalized.length() <= PREVIEW_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_LIMIT) + "……";
    }

    private int countWords(String text) {
        return text.replaceAll("\\s+", "").length();
    }
}
