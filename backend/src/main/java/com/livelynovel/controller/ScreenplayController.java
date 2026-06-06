package com.livelynovel.controller;

import com.livelynovel.common.Result;
import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.dto.ScreenplayConversionDetailDTO;
import com.livelynovel.model.dto.ScreenplayConvertRequestDTO;
import com.livelynovel.model.dto.SingleSceneConvertRequestDTO;
import com.livelynovel.service.LlmService;
import com.livelynovel.service.ScreenplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 剧本转换接口。
 * 详见技术方案文档 §4.5。
 */
@Tag(name = "剧本转换", description = "小说转剧本相关接口")
@RestController
@RequestMapping("/api/screenplay")
public class ScreenplayController {

    private final LlmService llmService;
    private final ScreenplayService screenplayService;

    public ScreenplayController(LlmService llmService, ScreenplayService screenplayService) {
        this.llmService = llmService;
        this.screenplayService = screenplayService;
    }

    @Operation(summary = "提交整本转换任务", description = "基于已存小说启动整本转换骨架，返回 SSE 事件流")
    @PostMapping(value = "/convert", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Object convert(@RequestBody ScreenplayConvertRequestDTO request) {
        if (request.getNovelId() == null || request.getNovelId().isBlank()) {
            return ResponseEntity.badRequest().body(Result.fail(40001, "novelId 不能为空"));
        }

        return screenplayService.convertNovel(
                request.getNovelId(),
                request.getScreenplayType()
        );
    }

    @Operation(summary = "获取整本转换详情", description = "根据 conversionId 返回已持久化的转换状态与场景结果")
    @GetMapping("/conversions/{conversionId}")
    public Result<ScreenplayConversionDetailDTO> getConversionDetail(@PathVariable String conversionId) {
        ScreenplayConversionDetailDTO detail = screenplayService.getConversionDetail(conversionId);
        if (detail == null) {
            return Result.fail(40401, "转换任务不存在");
        }
        return Result.ok(detail);
    }

    @Operation(summary = "获取最近完成转换", description = "根据 novelId 和 screenplayType 返回最近已完成的持久化转换")
    @GetMapping("/conversions/latest")
    public Result<ScreenplayConversionDetailDTO> getLatestCompletedConversion(
            @RequestParam String novelId,
            @RequestParam com.livelynovel.model.enums.ScreenplayTypeEnum screenplayType
    ) {
        ScreenplayConversionDetailDTO detail = screenplayService.getLatestCompletedConversion(novelId, screenplayType);
        if (detail == null) {
            return Result.fail(40401, "暂无已完成转换");
        }
        return Result.ok(detail);
    }

    @Operation(summary = "导出整本转换 YAML", description = "根据 conversionId 导出符合 YAML Schema 的剧本初稿")
    @GetMapping(value = "/conversions/{conversionId}/yaml", produces = "text/yaml;charset=UTF-8")
    public ResponseEntity<String> exportConversionYaml(@PathVariable String conversionId) {
        String yaml = screenplayService.exportConversionYaml(conversionId);
        if (yaml == null) {
            return ResponseEntity.status(404)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("转换任务不存在");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/yaml;charset=UTF-8"))
                .body(yaml);
    }

    /**
     * 单场转换（最小转换切片）。
     * 输入一段文本 → LLM → 返回结构化 SceneDTO。
     *
     * MVP 简化版：
     * - 不传入全局上下文（plotSummary、characters、prevSceneSummary）
     * - 不做章节分割、不做全局分析
     * - 不使用 SSE（一次性返回结果）
     * - 不做缓存、不做持久化
     */
    @Operation(summary = "单场转换", description = "将小说文本片段转换为结构化场景（最小转换切片）")
    @PostMapping("/convert-single")
    public Result<SceneDTO> convertSingle(@RequestBody SingleSceneConvertRequestDTO request) {
        // 参数校验
        if (request.getText() == null || request.getText().isBlank()) {
            return Result.fail(40001, "文本不能为空");
        }

        // 调用 LLM 服务
        SceneDTO scene = llmService.convertSingleScene(
                request.getText(),
                request.getScreenplayType()
        );

        return Result.ok(scene);
    }
}
