package com.livelynovel.controller;

import com.livelynovel.agent.AgentOrchestrator;
import com.livelynovel.common.Result;
import com.livelynovel.model.dto.ScreenplayConvertRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agent 转换", description = "Agent 化小说转剧本相关接口")
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentOrchestrator orchestrator;

    public AgentController(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Operation(summary = "提交 Agent 整本转换任务", description = "双轨 Agent 入口，返回 Agent SSE 事件流")
    @PostMapping(value = "/screenplay/convert", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Object convert(@RequestBody ScreenplayConvertRequestDTO request) {
        if (request.getNovelId() == null || request.getNovelId().isBlank()) {
            return ResponseEntity.badRequest().body(Result.fail(40001, "novelId 不能为空"));
        }
        return orchestrator.convertNovel(request.getNovelId(), request.getScreenplayType());
    }
}
