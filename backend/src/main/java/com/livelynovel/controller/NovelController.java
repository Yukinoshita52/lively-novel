package com.livelynovel.controller;

import com.livelynovel.common.Result;
import com.livelynovel.model.dto.NovelChaptersResultDTO;
import com.livelynovel.model.dto.NovelChapterDetailDTO;
import com.livelynovel.model.dto.NovelListResultDTO;
import com.livelynovel.model.dto.NovelUploadResultDTO;
import com.livelynovel.service.NovelService;
import com.livelynovel.common.exception.NovelValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 小说接口。
 * 详见技术方案文档 §4.4。
 */
@Tag(name = "小说", description = "小说解析相关接口")
@RestController
@RequestMapping("/api/novel")
public class NovelController {

    private final NovelService novelService;

    public NovelController(NovelService novelService) {
        this.novelService = novelService;
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

    @Operation(summary = "更新小说标题", description = "根据 novelId 更新已导入小说的作品标题")
    @PutMapping("/{id}/title")
    public Result<NovelChaptersResultDTO> updateTitle(@PathVariable("id") String novelId,
                                                      @RequestBody Map<String, String> request) {
        String title = request == null ? "" : request.get("title");
        NovelChaptersResultDTO result = novelService.updateTitle(novelId, title);
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
}
