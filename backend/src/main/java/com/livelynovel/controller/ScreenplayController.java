package com.livelynovel.controller;

import com.livelynovel.common.Result;
import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.dto.SingleSceneConvertRequestDTO;
import com.livelynovel.service.LlmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 剧本转换接口。
 * 详见技术方案文档 §4.5。
 */
@Tag(name = "剧本转换", description = "小说转剧本相关接口")
@RestController
@RequestMapping("/api/screenplay")
public class ScreenplayController {

    private final LlmService llmService;

    public ScreenplayController(LlmService llmService) {
        this.llmService = llmService;
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
