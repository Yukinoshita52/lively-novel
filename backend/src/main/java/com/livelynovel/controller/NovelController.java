package com.livelynovel.controller;

import com.livelynovel.common.Result;
import com.livelynovel.model.dto.ChapterDTO;
import com.livelynovel.model.dto.NovelChaptersResultDTO;
import com.livelynovel.model.dto.NovelChapterDetailDTO;
import com.livelynovel.model.dto.NovelListResultDTO;
import com.livelynovel.model.dto.NovelParseRequestDTO;
import com.livelynovel.model.dto.NovelParseResultDTO;
import com.livelynovel.model.dto.NovelUploadResultDTO;
import com.livelynovel.service.ChapterSplitter;
import com.livelynovel.service.NovelService;
import com.livelynovel.common.exception.NovelValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 小说接口。
 * 详见技术方案文档 §4.4。
 */
@Tag(name = "小说", description = "小说解析相关接口")
@RestController
@RequestMapping("/api/novel")
public class NovelController {

    /** 字数上限（去空白计）。详见 technical-design.md §4.4（错误码 40002）。 */
    private static final int MAX_WORD_COUNT = 200_000;

    /** 章节数下限。详见 technical-design.md §4.4（错误码 40003）。 */
    private static final int MIN_CHAPTERS = 3;

    private final ChapterSplitter chapterSplitter;
    private final NovelService novelService;

    public NovelController(ChapterSplitter chapterSplitter, NovelService novelService) {
        this.chapterSplitter = chapterSplitter;
        this.novelService = novelService;
    }

    /**
     * 粘贴文本并解析章节（无状态，不落库）。
     * 章节标题/字数由后端确定性切分得出（§5.2 步骤①）。
     */
    @Operation(summary = "粘贴文本解析章节", description = "确定性切分小说为章节列表（无状态，不落库）")
    @PostMapping("/parse")
    public Result<NovelParseResultDTO> parse(@RequestBody NovelParseRequestDTO request) {
        String text = request.getText();

        if (text == null || text.isBlank()) {
            return Result.fail(40001, "文本不能为空");
        }
        if (countWords(text) > MAX_WORD_COUNT) {
            return Result.fail(40002, "文本超过 " + MAX_WORD_COUNT + " 字上限");
        }

        List<ChapterDTO> chapters = chapterSplitter.split(text);
        if (chapters.size() < MIN_CHAPTERS) {
            return Result.fail(40003, "章节数不足（需 " + MIN_CHAPTERS + " 章以上），实际识别 "
                    + chapters.size() + " 章");
        }

        return Result.ok(buildResult(request.getTitle(), chapters));
    }

    /**
     * 上传 txt 并落库，返回 novelId/contentHash 与章节元信息。
     */
    @Operation(summary = "上传 txt 并解析章节", description = "上传小说 txt 文件，持久化后返回 novelId 与章节列表")
    @PostMapping("/upload")
    public Result<NovelUploadResultDTO> upload(@RequestParam(value = "title", required = false) String title,
                                               @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail(40001, "文件不能为空");
        }
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".txt")) {
            return Result.fail(40001, "仅支持上传 .txt 文件");
        }

        try {
            NovelUploadResultDTO result = novelService.uploadTxt(title, file);
            return Result.ok(result);
        } catch (NovelValidationException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 获取已导入小说的轻量历史列表。
     */
    @Operation(summary = "获取历史小说列表", description = "返回已导入小说的轻量列表")
    @GetMapping
    public Result<NovelListResultDTO> listNovels() {
        return Result.ok(novelService.listNovels());
    }

    /**
     * 回读已存小说的章节列表与 preview。
     */
    @Operation(summary = "获取已存小说章节", description = "根据 novelId 返回章节列表与预览")
    @GetMapping("/{id}/chapters")
    public Result<NovelChaptersResultDTO> getChapters(@PathVariable("id") String novelId) {
        NovelChaptersResultDTO result = novelService.getChapters(novelId);
        if (result == null) {
            return Result.fail(40401, "小说不存在");
        }
        return Result.ok(result);
    }

    /**
     * 回读已存小说的单章正文。
     */
    @Operation(summary = "获取单章正文", description = "根据 novelId 与 chapterIndex 返回单章正文")
    @GetMapping("/{id}/chapters/{chapterIndex}")
    public Result<NovelChapterDetailDTO> getChapterDetail(@PathVariable("id") String novelId,
                                                          @PathVariable("chapterIndex") int chapterIndex) {
        NovelChapterDetailDTO result = novelService.getChapterDetail(novelId, chapterIndex);
        if (result == null) {
            return Result.fail(40401, "章节不存在");
        }
        return Result.ok(result);
    }

    /** 组装响应体；正文不回传（仅保留章节元信息）。 */
    private NovelParseResultDTO buildResult(String title, List<ChapterDTO> chapters) {
        int totalWordCount = chapters.stream().mapToInt(ChapterDTO::getWordCount).sum();
        chapters.forEach(c -> c.setContent(null));

        NovelParseResultDTO result = new NovelParseResultDTO();
        result.setTitle(title);
        result.setTotalChapters(chapters.size());
        result.setTotalWordCount(totalWordCount);
        result.setChapters(chapters);
        return result;
    }

    /** 字数：去掉所有空白字符后的字符数。 */
    private int countWords(String text) {
        return text.replaceAll("\\s+", "").length();
    }
}
