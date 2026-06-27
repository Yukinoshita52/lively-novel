package com.livelynovel.controller;

import com.livelynovel.agent.AgentOrchestrator;
import com.livelynovel.agent.AgentTraceService;
import com.livelynovel.common.Result;
import com.livelynovel.model.dto.AgentTraceDTO;
import com.livelynovel.model.dto.ScreenplayConvertRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agent 转换", description = "Agent 化小说转剧本相关接口")
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentOrchestrator orchestrator;
    private final AgentTraceService traceService;

    public AgentController(AgentOrchestrator orchestrator, AgentTraceService traceService) {
        this.orchestrator = orchestrator;
        this.traceService = traceService;
    }

    @Operation(summary = "提交 Agent 整本转换任务", description = "双轨 Agent 入口，返回 Agent SSE 事件流")
    @PostMapping(value = "/screenplay/convert", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Object convert(@RequestBody ScreenplayConvertRequestDTO request) {
        if (request.getNovelId() == null || request.getNovelId().isBlank()) {
            return ResponseEntity.badRequest().body(Result.fail(40001, "novelId 不能为空"));
        }
        return orchestrator.convertNovel(request.getNovelId(), request.getScreenplayType());
    }

    @Operation(summary = "回读 Agent run trace", description = "按 runId 聚合返回 Agent step、guardrail 与 tool call trace")
    @GetMapping("/runs/{runId}/trace")
    public Result<AgentTraceDTO> getTrace(@PathVariable String runId) {
        if (runId == null || runId.isBlank()) {
            return Result.fail(40001, "runId 不能为空");
        }
        return traceService.getTrace(runId)
                .map(Result::ok)
                .orElseGet(() -> Result.fail(40401, "Agent run 不存在"));
    }
}
