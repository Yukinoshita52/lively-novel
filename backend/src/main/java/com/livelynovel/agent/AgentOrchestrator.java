package com.livelynovel.agent;

import com.livelynovel.model.entity.AgentRunEntity;
import com.livelynovel.model.entity.AgentToolCallEntity;
import com.livelynovel.model.enums.AgentRunStatusEnum;
import com.livelynovel.model.enums.AgentToolCallStatusEnum;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.repository.AgentRunRepository;
import com.livelynovel.repository.AgentToolCallRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
public class AgentOrchestrator {

    private static final String FIXED_PLAN_TOOL = ScreenplayAgentTools.RUN_EXISTING_SCREENPLAY_CONVERSION;

    private final AgentRunRepository runRepository;
    private final AgentToolCallRepository toolCallRepository;
    private final ToolRegistry toolRegistry;
    private final AgentSseEventFactory eventFactory;

    public AgentOrchestrator(
            AgentRunRepository runRepository,
            AgentToolCallRepository toolCallRepository,
            ToolRegistry toolRegistry,
            AgentSseEventFactory eventFactory
    ) {
        this.runRepository = runRepository;
        this.toolCallRepository = toolCallRepository;
        this.toolRegistry = toolRegistry;
        this.eventFactory = eventFactory;
    }

    public SseEmitter convertNovel(String novelId, ScreenplayTypeEnum screenplayType) {
        SseEmitter emitter = new SseEmitter(0L);
        AgentRunEntity run = createRun(novelId, screenplayType);
        send(emitter, "agent_started", run.getId(), null, null, "Agent run started");
        send(emitter, "plan_created", run.getId(), null, null, "Fixed conversion plan loaded");
        executeFixedPlan(emitter, run);
        return emitter;
    }

    private AgentRunEntity createRun(String novelId, ScreenplayTypeEnum screenplayType) {
        AgentRunEntity run = new AgentRunEntity();
        run.setId(generateRunId());
        run.setUserGoal("将小说转换为动画剧本");
        run.setNovelId(novelId);
        run.setScreenplayType(screenplayType);
        run.setStatus(AgentRunStatusEnum.RUNNING);
        run.setCurrentStepIndex(0);
        return runRepository.save(run);
    }

    private void executeFixedPlan(SseEmitter emitter, AgentRunEntity run) {
        AgentTool tool = toolRegistry.getRequired(FIXED_PLAN_TOOL);
        AgentToolCallEntity toolCall = createToolCall(run, tool);
        send(emitter, "tool_call_started", run.getId(), toolCall.getId(), run.getConversionId(),
                tool.name() + " started");
        try {
            AgentToolResult result = tool.executor().execute(new AgentToolContext(
                    run.getId(),
                    run.getNovelId(),
                    run.getScreenplayType(),
                    run.getConversionId()
            ));
            markToolCallCompleted(toolCall, result);
            markRunCompleted(run, result);
            send(emitter, "tool_call_completed", run.getId(), toolCall.getId(), run.getConversionId(),
                    tool.name() + " completed");
            send(emitter, "agent_completed", run.getId(), null, run.getConversionId(), "Agent run completed");
            emitter.complete();
        } catch (Exception e) {
            markToolCallFailed(toolCall, e);
            markRunFailed(run, e);
            send(emitter, "agent_failed", run.getId(), toolCall.getId(), run.getConversionId(), e.getMessage());
            emitter.complete();
        }
    }

    private AgentToolCallEntity createToolCall(AgentRunEntity run, AgentTool tool) {
        AgentToolCallEntity toolCall = new AgentToolCallEntity();
        toolCall.setId(generateToolCallId());
        toolCall.setRunId(run.getId());
        toolCall.setToolName(tool.name());
        toolCall.setSideEffectLevel(tool.sideEffectLevel());
        toolCall.setInputJson("{\"novelId\":\"" + run.getNovelId() + "\",\"screenplayType\":\""
                + run.getScreenplayType() + "\"}");
        toolCall.setStatus(AgentToolCallStatusEnum.RUNNING);
        toolCall.setStartedAt(Instant.now());
        return toolCallRepository.save(toolCall);
    }

    private void markToolCallCompleted(AgentToolCallEntity toolCall, AgentToolResult result) {
        toolCall.setStatus(AgentToolCallStatusEnum.COMPLETED);
        toolCall.setOutputJson(result.outputJson());
        toolCall.setCompletedAt(Instant.now());
        toolCallRepository.save(toolCall);
    }

    private void markRunCompleted(AgentRunEntity run, AgentToolResult result) {
        run.setConversionId(result.conversionId());
        run.setStatus(AgentRunStatusEnum.COMPLETED);
        run.setCompletedAt(Instant.now());
        run.setFinalArtifactRef(result.conversionId());
        runRepository.save(run);
    }

    private void markToolCallFailed(AgentToolCallEntity toolCall, Exception error) {
        toolCall.setStatus(AgentToolCallStatusEnum.FAILED);
        toolCall.setErrorMessage(error.getMessage());
        toolCall.setCompletedAt(Instant.now());
        toolCallRepository.save(toolCall);
    }

    private void markRunFailed(AgentRunEntity run, Exception error) {
        run.setStatus(AgentRunStatusEnum.FAILED);
        run.setCompletedAt(Instant.now());
        run.setErrorMessage(error.getMessage());
        runRepository.save(run);
    }

    private void send(
            SseEmitter emitter,
            String eventName,
            String runId,
            String toolCallId,
            String conversionId,
            String message
    ) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(eventFactory.payload(eventName, runId, toolCallId, conversionId, message)));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private String generateRunId() {
        return "ar-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String generateToolCallId() {
        return "atc-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
